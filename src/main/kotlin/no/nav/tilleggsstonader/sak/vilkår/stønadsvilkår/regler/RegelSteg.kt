package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler

import no.nav.tilleggsstonader.libs.feil.feil

/**
 * Ett [RegelSteg] er en regel med ett spørsmål med flere svar som mapper til en [SvarRegel]
 * @param regelId er id for spørsmålet
 * @param svarMapping er en mapping mellom svarId og regel for det svaret
 * @param avhengerAvHovedregler brukes når denne regelen er avhengig av at andre, uavhengige hovedregler
 * (som ikke leder til denne regelen via [SvarRegel.NesteRegel]) er besvart Ja. Brukes kun som metadata for
 * å styre synlighet i frontend (se [no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelInfo]) og
 * påvirker ikke selve evalueringen av svar.
 *
 * Eks
 * SPØRSMÅL_1 har 2 ulike svar,
 *  SVAR_1 som peker mot att vilkåret er OPPFYLT
 *  SVAR_2 som peker til ett nytt spørsmål
 * {
 *  [regelId]: "SPØRSMÅL_1",
 *  [erHovedregel]: true
 *  [svarMapping]: {
 *      "SVAR_1": SluttSvarRegel.OPPFYLT,
 *      "SVAR_2": {
 *          "regelId": "SPØRSMÅL_2",
 *          "begrunnelseType": "UTEN"
 *      }
 *  }
 * }
 */
data class RegelSteg(
    val regelId: RegelId,
    val erHovedregel: Boolean,
    val svarMapping: Map<SvarId, SvarRegel>,
    val avhengerAvHovedregler: Set<RegelId> = emptySet(),
) {
    fun svarMapping(svarId: SvarId): SvarRegel = svarMapping[svarId] ?: feil("Finner ikke svarId=$svarId for regelId=$regelId")
}

/**
 * Util for å sette opp svarmapping med [SvarId.JA] og [SvarId.NEI] som svarsalternativ
 */
fun jaNeiSvarRegel(
    hvisJa: SvarRegel = SluttSvarRegel.OPPFYLT,
    hvisNei: SvarRegel = SluttSvarRegel.IKKE_OPPFYLT,
): Map<SvarId, SvarRegel> = mapOf(SvarId.JA to hvisJa, SvarId.NEI to hvisNei)
