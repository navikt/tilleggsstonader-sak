package no.nav.tilleggsstonader.sak.vilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.regler.BegrunnelseType
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.regler.SvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel

object RegelValidering {

    fun validerVilkår(
        vilkårsregel: Vilkårsregel,
        oppdatering: List<DelvilkårDto>,
        tidligereDelvilkår: List<Delvilkår>,
    ) {
        validerAlleDelvilkårHarMinimumEttSvar(vilkårsregel.vilkårType, oppdatering)
        validerAlleHovedreglerFinnesMed(vilkårsregel, oppdatering, tidligereDelvilkår)

        oppdatering.forEach { delvilkårsvurderingDto ->
            validerDelvilkår(vilkårsregel, delvilkårsvurderingDto)
        }
    }

    /**
     * Validerer att begrunnelse er ifylt hvis [SvarRegel.begrunnelseType]=[BegrunnelseType.PÅKREVD]
     */
    fun manglerPåkrevdBegrunnelse(svarRegel: SvarRegel, vurdering: VurderingDto): Boolean =
        svarRegel.begrunnelseType == BegrunnelseType.PÅKREVD && vurdering.begrunnelse?.trim().isNullOrEmpty()

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
        oppdatering: List<DelvilkårDto>,
    ) {
        oppdatering.forEach { vurdering ->
            feilHvis(vurdering.vurderinger.isEmpty()) { "Savner svar for en av delvilkåren for vilkår=$vilkårType" }
        }
    }

    private fun validerAlleHovedreglerFinnesMed(
        vilkårsregel: Vilkårsregel,
        delvilkår: List<DelvilkårDto>,
        tidligereDelvilkårsvurderinger: List<Delvilkår>,
    ) {
        val aktuelleDelvilkår = aktuelleDelvilkår(tidligereDelvilkårsvurderinger)
        val delvilkårRegelIdn = delvilkår.map { it.hovedregel() }
        val aktuelleHvovedregler = vilkårsregel.hovedregler.filter { aktuelleDelvilkår.contains(it) }
        feilHvis(!aktuelleHvovedregler.containsAll(delvilkårRegelIdn)) {
            "Delvilkårsvurderinger savner svar på hovedregler - hovedregler=$aktuelleHvovedregler delvilkår=$delvilkårRegelIdn"
        }
        feilHvis(delvilkårRegelIdn.size != aktuelleHvovedregler.size) {
            "Feil i antall regler dto har ${delvilkårRegelIdn.size} " +
                "mens vilkår har ${aktuelleHvovedregler.size} aktuelle delvilkår"
        }
    }

    private fun aktuelleDelvilkår(tidligereDelvilkårsvurderinger: List<Delvilkår>): Set<RegelId> {
        return tidligereDelvilkårsvurderinger
            .filter { it.resultat != Vilkårsresultat.IKKE_AKTUELL }
            .map { it.hovedregel }
            .toSet()
    }

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
