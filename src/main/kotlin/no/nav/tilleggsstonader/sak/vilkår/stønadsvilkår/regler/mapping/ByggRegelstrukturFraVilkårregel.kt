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
    fun Vilkårsregel.tilRegelstruktur(): Map<RegelId, RegelInfo> {
        val alleRegler = this.regler

        return this.hovedregler
            .map { hovedregel ->
                val hierarki = byggHierarki(hovedregel, alleRegler)
                hierarki.mapValues { (regelId, plasseringIHierarki) ->
                    tilRegelInfo(alleRegler[regelId]!!, plasseringIHierarki, hovedregel)
                }
            }
            // Bør returnere List<RegelId, Map<RegelId, RegelInfo>>? Dersom flere hovedregler som er innom samme svar, vil kun et svar og dens plassering i hierarki finnes under
            .flatMap { it.entries }
            .associate { it.key to it.value }
    }

    private fun tilRegelInfo(
        regel: RegelSteg,
        plasseringIHierarki: Int,
        hovedregelId: RegelId,
    ): RegelInfo =
        RegelInfo(
            erHovedregel = regel.erHovedregel,
            nullstillingsinfo =
                Nullstillingsinfo(
                    plasseringIHierarki = plasseringIHierarki,
                    hovedregel = hovedregelId,
                ),
            svaralternativer = regel.svarMapping.tilSvaralternativ(),
        )

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
        hovedregel: RegelId,
        alleRegler: Map<RegelId, RegelSteg>,
    ): Map<RegelId, Int> {
        val regelHierarkiListe = lagRegelHierarkiListe(hovedregel, alleRegler)
        val filtrertHierarkiListe = mutableListOf<RegelId>()
        regelHierarkiListe.forEach {
            if (!filtrertHierarkiListe.contains(it)) {
                filtrertHierarkiListe.add(it)
            } else {
                filtrertHierarkiListe.remove(it)
                filtrertHierarkiListe.add(it)
            }
        }

        return filtrertHierarkiListe.mapIndexed { index, regelId -> regelId to index + 1 }.toMap()
    }

    private fun lagRegelHierarkiListe(
        regelId: RegelId,
        alleRegler: Map<RegelId, RegelSteg>,
    ): List<RegelId> =
        listOf(regelId) +
            alleRegler[regelId].mapUtNesteRegel().map { lagRegelHierarkiListe(it.regelId, alleRegler) }.flatten()

    private fun RegelSteg?.mapUtNesteRegel(): List<NesteRegel> {
        if (this == null) return emptyList()
        return this.svarMapping
            .values
            .filterIsInstance<NesteRegel>()
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
