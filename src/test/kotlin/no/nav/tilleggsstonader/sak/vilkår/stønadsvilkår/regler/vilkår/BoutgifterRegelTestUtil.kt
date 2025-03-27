package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId

object BoutgifterRegelTestUtil {
    fun oppfylteDelvilkårUtgifterOvernatting() =
        listOf(
            delvilkår(Vurdering(RegelId.NØDVENDIGE_MERUTGIFTER, SvarId.JA)),
        )

    fun oppfylteDelvilkårFasteUtgifterEnBolig() =
        listOf(
            delvilkår(Vurdering(RegelId.HØYERE_BOUTGIFTER_SAMMENLIGNET_MED_TIDLIGERE, SvarId.JA)),
            delvilkår(Vurdering(RegelId.NØDVENDIG_Å_BO_NÆRMERE_AKTIVITET, SvarId.JA)),
            delvilkår(Vurdering(RegelId.RETT_TIL_BOSTØTTE, SvarId.NEI)),
            delvilkår(Vurdering(RegelId.HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER, SvarId.NEI)),
        )

    fun oppfylteDelvilkårFasteUtgifterToBoliger() =
        listOf(
            delvilkår(Vurdering(RegelId.NØDVENDIG_Å_BO_NÆRMERE_AKTIVITET, SvarId.JA)),
            delvilkår(Vurdering(RegelId.DOKUMENTERT_UTGIFTER_BOLIG, SvarId.JA)),
            delvilkår(Vurdering(RegelId.HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER, SvarId.NEI)),
        )

    fun ikkeOppfylteDelvilkårUtgifterOvernatting() =
        listOf(
            delvilkår(Vurdering(RegelId.NØDVENDIGE_MERUTGIFTER, SvarId.NEI, "begrunnelse")),
        )

    fun ikkeOppfylteDelvilkårFasteUtgifterEnBolig() =
        listOf(
            delvilkår(Vurdering(RegelId.HØYERE_BOUTGIFTER_SAMMENLIGNET_MED_TIDLIGERE, SvarId.NEI, "begrunnelse")),
            delvilkår(Vurdering(RegelId.NØDVENDIG_Å_BO_NÆRMERE_AKTIVITET, SvarId.JA)),
            delvilkår(Vurdering(RegelId.RETT_TIL_BOSTØTTE, SvarId.NEI)),
            delvilkår(Vurdering(RegelId.HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER, SvarId.NEI)),
        )

    fun ikkeOppfylteDelvilkårFasteUtgifterToBoliger() =
        listOf(
            delvilkår(Vurdering(RegelId.NØDVENDIG_Å_BO_NÆRMERE_AKTIVITET, SvarId.NEI, "begrunnelse")),
            delvilkår(Vurdering(RegelId.DOKUMENTERT_UTGIFTER_BOLIG, SvarId.JA)),
            delvilkår(Vurdering(RegelId.HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER, SvarId.NEI)),
        )

    fun oppfylteDelvilkårUtgifterOvernattingDto() = oppfylteDelvilkårUtgifterOvernatting().map { it.tilDto() }

    fun oppfylteDelvilkårFasteUtgifterEnBoligDto() = oppfylteDelvilkårFasteUtgifterEnBolig().map { it.tilDto() }

    fun oppfylteDelvilkårFasteUtgifterToBoligerDto() = oppfylteDelvilkårFasteUtgifterToBoliger().map { it.tilDto() }

    fun ikkeOppfylteDelvilkårUtgifterOvernattingDto() = ikkeOppfylteDelvilkårUtgifterOvernatting().map { it.tilDto() }

    fun ikkeOppfylteDelvilkårFasteUtgifterEnBoligDto() = ikkeOppfylteDelvilkårFasteUtgifterEnBolig().map { it.tilDto() }

    fun ikkeOppfylteDelvilkårFasteUtgifterToBoligerDto() = ikkeOppfylteDelvilkårFasteUtgifterToBoliger().map { it.tilDto() }

    private fun delvilkår(vararg vurderinger: Vurdering) =
        Delvilkår(
            resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            vurderinger = vurderinger.toList(),
        )
}
