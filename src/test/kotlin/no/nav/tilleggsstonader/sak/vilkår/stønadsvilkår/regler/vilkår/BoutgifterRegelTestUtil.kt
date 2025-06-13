package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId

object BoutgifterRegelTestUtil {
    fun oppfylteDelvilkårUtgifterOvernatting() =
        listOf(
            delvilkår(
                Vurdering(
                    regelId = RegelId.NØDVENDIGE_MERUTGIFTER,
                    svar = SvarId.JA,
                    begrunnelse = "En begrunnelse på delvilkåret",
                ),
            ),
            delvilkår(Vurdering(regelId = RegelId.DOKUMENTERT_UTGIFTER_OVERNATTING, svar = SvarId.JA)),
            delvilkår(Vurdering(regelId = RegelId.DOKUMENTERT_DELTAKELSE, svar = SvarId.JA)),
            delvilkår(Vurdering(regelId = RegelId.HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER, svar = SvarId.NEI)),
        )

    fun delvilkårFremtidigeUtgifter() =
        listOf(
            delvilkår(Vurdering(regelId = RegelId.NØDVENDIGE_MERUTGIFTER, svar = null)),
            delvilkår(Vurdering(regelId = RegelId.DOKUMENTERT_UTGIFTER_OVERNATTING, svar = null)),
            delvilkår(Vurdering(regelId = RegelId.DOKUMENTERT_DELTAKELSE, svar = null)),
            delvilkår(Vurdering(RegelId.HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER, null)),
        )

    fun oppfylteDelvilkårLøpendeUtgifterEnBolig() =
        listOf(
            delvilkår(Vurdering(RegelId.HØYERE_BOUTGIFTER_SAMMENLIGNET_MED_TIDLIGERE, SvarId.JA)),
            delvilkår(Vurdering(RegelId.NØDVENDIG_Å_BO_NÆRMERE_AKTIVITET, SvarId.JA, begrunnelse = "påkrevd")),
            delvilkår(Vurdering(RegelId.RETT_TIL_BOSTØTTE, SvarId.NEI)),
            delvilkår(Vurdering(RegelId.HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER, SvarId.NEI)),
        )

    fun oppfylteDelvilkårLøpendeUtgifterToBoliger() =
        listOf(
            delvilkår(Vurdering(RegelId.NØDVENDIG_Å_BO_NÆRMERE_AKTIVITET, SvarId.JA, begrunnelse = "påkrevd")),
            delvilkår(Vurdering(RegelId.DOKUMENTERT_UTGIFTER_BOLIG, SvarId.JA)),
            delvilkår(Vurdering(RegelId.HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER, SvarId.NEI)),
        )

    fun oppfylteDelvilkårLøpendeUtgifterToBoligerHøyereUtgifterHelsemessigÅrsaker() =
        listOf(
            delvilkår(Vurdering(RegelId.NØDVENDIG_Å_BO_NÆRMERE_AKTIVITET, SvarId.JA, "påkrevd")),
            delvilkår(Vurdering(RegelId.DOKUMENTERT_UTGIFTER_BOLIG, SvarId.JA)),
            delvilkår(Vurdering(RegelId.HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER, SvarId.JA, "begrunnelse")),
        )

    fun ikkeOppfylteDelvilkårUtgifterOvernatting() =
        listOf(
            delvilkår(Vurdering(RegelId.NØDVENDIGE_MERUTGIFTER, SvarId.NEI, "begrunnelse")),
            delvilkår(Vurdering(RegelId.DOKUMENTERT_DELTAKELSE, SvarId.JA)),
            delvilkår(Vurdering(RegelId.DOKUMENTERT_UTGIFTER_OVERNATTING, SvarId.JA)),
            delvilkår(Vurdering(RegelId.HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER, SvarId.NEI)),
        )

    fun ikkeOppfylteDelvilkårLøpendeUtgifterEnBolig() =
        listOf(
            delvilkår(Vurdering(RegelId.HØYERE_BOUTGIFTER_SAMMENLIGNET_MED_TIDLIGERE, SvarId.NEI, "begrunnelse")),
            delvilkår(Vurdering(RegelId.NØDVENDIG_Å_BO_NÆRMERE_AKTIVITET, SvarId.JA, begrunnelse = "påkrevd")),
            delvilkår(Vurdering(RegelId.RETT_TIL_BOSTØTTE, SvarId.NEI)),
            delvilkår(Vurdering(RegelId.HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER, SvarId.NEI)),
        )

    fun ikkeOppfylteDelvilkårLøpendeUtgifterToBoliger() =
        listOf(
            delvilkår(Vurdering(RegelId.NØDVENDIG_Å_BO_NÆRMERE_AKTIVITET, SvarId.NEI, "begrunnelse")),
            delvilkår(Vurdering(RegelId.DOKUMENTERT_UTGIFTER_BOLIG, SvarId.JA)),
            delvilkår(Vurdering(RegelId.HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER, SvarId.NEI)),
        )

    private fun delvilkår(vararg vurderinger: Vurdering) =
        Delvilkår(
            resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            vurderinger = vurderinger.toList(),
        )
}
