package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.NesteRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Nullstillingsinfo
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelInfo
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarAlternativ
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel

object ByggRegelstrukturFraVilkårregel {
    /**
     * Se json-eksempler for hvordan regelstrukturen ser ut:
     * src/test/resources/vilkår/regelstruktur
     */
    fun Vilkårsregel.tilRegelstruktur(): RegelstrukturDto {
        val alleRegler = this.regler

        val hierarki =
            this.hovedregler.associate {
                it to
                    byggHierarki(
                        startRegelId = it,
                        alleRegler = alleRegler,
                        hierarki = mutableMapOf<RegelId, Int>(),
                    )
            }

        return hierarki
            .map { (hovedregelId, hierarkiForHovedregel) ->
                hierarkiForHovedregel.mapValues { (regelId, plasseringIHierarki) ->
                    val regel = alleRegler[regelId] ?: error("Mangler steg for $regelId")
                    RegelInfo(
                        erHovedregel = regel.erHovedregel,
                        nullstillingsinfo =
                            Nullstillingsinfo(
                                plasseringIHierarki = plasseringIHierarki,
                                hovedregel = hovedregelId,
                            ),
                        svaralternativer = regel.svarMapping.tilSvaralternativ(),
                    )
                }
            }.flatMap { it.entries }
            .associate { it.key to it.value }
    }

    /**
     * Traverserer regelTreet fra en startRegelId (hovedregelId) og videre til alle underregler den eventuelt trigger.
     * Dersom en regel kun har sluttnoder som svaralternativer så vil traverseringen stoppe.
     *
     * Plassering i hierarki reflekterer *maks* antall regler som må være besvart før regelen blir synlig.
     * Den brukes for å nullstille svar i frontend.
     *
     * Eksempel - Daglig reise:
     *
     * Avstands > 6 = ja -> offentlig transport
     * Avstands > 6 = nei -> oppfyller unntakskrav = ja -> offentlig transport
     *
     * Her skal regelen for offentlig transport få plasseringIHierarki = 3 fordi den både skal nullstilles
     * dersom avstandspm og unntak endrer svar.
     */
    private fun byggHierarki(
        startRegelId: RegelId,
        alleRegler: Map<RegelId, RegelSteg>,
        plasseringIHierarki: Int = 1,
        hierarki: MutableMap<RegelId, Int> = mutableMapOf(),
    ): Map<RegelId, Int> {
        val eksisterende = hierarki[startRegelId]
        if (eksisterende == null || plasseringIHierarki > eksisterende) {
            hierarki[startRegelId] = plasseringIHierarki
            alleRegler[startRegelId]
                ?.svarMapping
                ?.values
                ?.filterIsInstance<NesteRegel>()
                ?.forEach { byggHierarki(it.regelId, alleRegler, plasseringIHierarki + 1, hierarki) }
        }
        return hierarki
    }

    private fun Map<SvarId, SvarRegel>.tilSvaralternativ(): List<SvarAlternativ> =
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
                        triggerFakta = svarRegel.triggerFakta,
                    )
            }
        }
}
