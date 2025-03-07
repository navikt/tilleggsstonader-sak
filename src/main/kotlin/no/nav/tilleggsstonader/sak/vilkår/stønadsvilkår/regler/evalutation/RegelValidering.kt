package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.BegrunnelseType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel

object RegelValidering {
    fun validerVilkår(
        vilkårsregel: Vilkårsregel,
        oppdatertDelvilkårsett: List<DelvilkårDto>,
        tidligereDelvilkårsett: List<Delvilkår>,
    ) {
        validerAlleDelvilkårHarMinimumEttSvar(vilkårsregel.vilkårType, oppdatertDelvilkårsett)
        validerAlleHovedreglerFinnesMed(vilkårsregel, oppdatertDelvilkårsett, tidligereDelvilkårsett)

        oppdatertDelvilkårsett.forEach { oppdatertDelvilkår ->
            validerDelvilkår(vilkårsregel, oppdatertDelvilkår)
        }
    }

    /**
     * Validerer att begrunnelse er ifylt hvis [SvarRegel.begrunnelseType]=[BegrunnelseType.PÅKREVD]
     */
    fun manglerPåkrevdBegrunnelse(
        svarRegel: SvarRegel,
        vurdering: VurderingDto,
    ): Boolean = svarRegel.begrunnelseType == BegrunnelseType.PÅKREVD && vurdering.begrunnelse?.trim().isNullOrEmpty()

    /**
     * Kaster feil hvis
     *
     * * ett [Vurdering] savner svar OG ikke er det siste svaret, eks svaren [ja, null, nei]
     *
     * * har begrunnelse men er [BegrunnelseType.UTEN]
     *
     * * ett svar er av typen [SluttSvarRegel] men att det finnes flere svar, eks [ja, nei, ja],
     *   hvor det andre svaret egentlige er type [SluttSvarRegel]
     *
     */
    private fun validerDelvilkår(
        vilkårsregel: Vilkårsregel,
        delvilkårDto: DelvilkårDto,
    ) {
        val vilkårType = vilkårsregel.vilkårType
        delvilkårDto.vurderinger.forEachIndexed { index, svar ->
            val (regelId: RegelId, svarId: SvarId?, _) = svar
            val regelMapping = vilkårsregel.regel(regelId)
            val erIkkeSisteSvaret = index != (delvilkårDto.vurderinger.size - 1)

            if (svarId == null) {
                feilHvis(erIkkeSisteSvaret) {
                    "Mangler svar på ett spørsmål som ikke er siste besvarte spørsmålet vilkårType=$vilkårType regelId=$regelId"
                }
            } else {
                val svarMapping = regelMapping.svarMapping(svarId)
                validerManglerBegrunnelseHvisUtenBegrunnelse(vilkårType, svarMapping, svar)
                feilHvis(svarMapping is SluttSvarRegel && erIkkeSisteSvaret) {
                    "Finnes ikke noen flere regler, men finnes flere svar vilkårType=$vilkårType svarId=$svarId"
                }
            }
        }
    }

    /**
     * Skal validere att man sender inn minimum ett svar for ett delvilkår
     * Når backend initierar [Delvilkår] så legges ett første svar in med regelId(hovedregel) for hvert delvilkår
     */
    private fun validerAlleDelvilkårHarMinimumEttSvar(
        vilkårType: VilkårType,
        delvilkårsett: List<DelvilkårDto>,
    ) {
        delvilkårsett.forEach { delvilkår ->
            feilHvis(delvilkår.vurderinger.isEmpty()) { "Savner svar for en av delvilkåren for vilkår=$vilkårType" }
        }
    }

    private fun validerAlleHovedreglerFinnesMed(
        vilkårsregel: Vilkårsregel,
        delvilkår: List<DelvilkårDto>,
        tidligereDelvilkårsett: List<Delvilkår>,
    ) {
        val aktuelleDelvilkår = aktuelleDelvilkår(tidligereDelvilkårsett)
        val delvilkårRegelIdn = delvilkår.map { it.hovedregel() }
        val aktuelleHvovedregler = vilkårsregel.hovedregler.filter { aktuelleDelvilkår.contains(it) }
        feilHvis(!aktuelleHvovedregler.containsAll(delvilkårRegelIdn)) {
            "Delvilkårsett mangler svar på hovedregler - hovedregler=$aktuelleHvovedregler delvilkår=$delvilkårRegelIdn"
        }
        feilHvis(delvilkårRegelIdn.size != aktuelleHvovedregler.size) {
            "Feil i antall regler dto har ${delvilkårRegelIdn.size} " +
                "mens vilkår har ${aktuelleHvovedregler.size} aktuelle delvilkår"
        }
    }

    private fun aktuelleDelvilkår(tidligereDelvilkårsett: List<Delvilkår>): Set<RegelId> =
        tidligereDelvilkårsett
            .filter { it.resultat != Vilkårsresultat.IKKE_AKTUELL }
            .map { it.hovedregel }
            .toSet()

    /**
     * Valider att begrunnelse i svaret savnes hvis [SvarRegel.begrunnelseType]=[BegrunnelseType.UTEN]
     */
    private fun validerManglerBegrunnelseHvisUtenBegrunnelse(
        vilkårType: VilkårType,
        svarMapping: SvarRegel,
        vurdering: VurderingDto,
    ) {
        if (svarMapping.begrunnelseType == BegrunnelseType.UTEN && !vurdering.begrunnelse.isNullOrEmpty()) {
            throw Feil(
                "Begrunnelse for vilkårType=$vilkårType regelId=${vurdering.regelId} " +
                    "svarId=${vurdering.svar} skal ikke ha begrunnelse",
            )
        }
    }
}
