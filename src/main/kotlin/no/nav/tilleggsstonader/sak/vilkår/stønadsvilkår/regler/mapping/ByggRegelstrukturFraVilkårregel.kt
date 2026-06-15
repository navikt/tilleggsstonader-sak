package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.NesteRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelInfo
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarAlternativ
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import kotlin.collections.forEach
import kotlin.collections.orEmpty

object ByggRegelstrukturFraVilkårregel {
    /**
     * Se json-eksempler for hvordan regelstrukturen ser ut:
     * src/test/resources/vilkår/regelstruktur
     */
    fun Vilkårsregel.tilRegelstruktur(): RegelstrukturDto {
        val alleRegler = this.regler

        val alleReglerMedDirekteNesteRegel =
            alleRegler.mapValues { (_, steg) ->
                steg.svarMapping.values
                    .filterIsInstance<NesteRegel>()
                    .map { it.regelId }
            }

        return alleRegler.mapValues { (regelId, regel) ->
            RegelInfo(
                erHovedregel = regel.erHovedregel,
                svaralternativer = regel.svarMapping.tilSvaralternativer(),
                reglerSomMåNullstilles = finnEtterkommere(alleReglerMedDirekteNesteRegel, start = regelId).toList(),
            )
        }
    }

    private fun finnEtterkommere(
        reglerMedDirekteNesteRegel: Map<RegelId, List<RegelId>>,
        start: RegelId,
        besøkt: MutableSet<RegelId> = mutableSetOf(),
    ): Set<RegelId> {
        val neste = reglerMedDirekteNesteRegel[start].orEmpty()
        neste.forEach { child ->
            if (besøkt.add(child)) {
                finnEtterkommere(reglerMedDirekteNesteRegel, child, besøkt)
            }
        }
        return besøkt
    }

    private fun Map<SvarId, SvarRegel>.tilSvaralternativer(): List<SvarAlternativ> =
        this.map { (svarId, svarRegel) ->
            when (svarRegel) {
                is NesteRegel ->
                    SvarAlternativ(
                        svarId = svarId,
                        begrunnelseType = svarRegel.begrunnelseType,
                        nesteRegelId = svarRegel.regelId,
                    )

                is SluttSvarRegel ->
                    SvarAlternativ(
                        svarId = svarId,
                        begrunnelseType = svarRegel.begrunnelseType,
                        tilhørendeFaktaType = svarRegel.tilhørendeFaktaType,
                    )
            }
        }
}
