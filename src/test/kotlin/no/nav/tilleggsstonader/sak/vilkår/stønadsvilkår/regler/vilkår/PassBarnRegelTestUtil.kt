package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId

object PassBarnRegelTestUtil {

    fun oppfylteDelvilkårPassBarn() = listOf(
        delvilkår(Vurdering(RegelId.HAR_FULLFØRT_FJERDEKLASSE, SvarId.NEI, "en begrunnelse")),
        delvilkår(Vurdering(RegelId.ANNEN_FORELDER_MOTTAR_STØTTE, SvarId.NEI)),
        delvilkår(Vurdering(RegelId.UTGIFTER_DOKUMENTERT, SvarId.JA)),
    )

    fun oppfylteDelvilkårPassBarnDto() = oppfylteDelvilkårPassBarn().map { it.tilDto() }

    private fun delvilkår(vararg vurderinger: Vurdering) = Delvilkår(
        resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
        vurderinger = vurderinger.toList(),
    )
}
