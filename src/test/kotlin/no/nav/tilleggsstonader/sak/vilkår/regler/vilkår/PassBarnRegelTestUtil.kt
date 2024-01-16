package no.nav.tilleggsstonader.sak.vilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.SvarId

object PassBarnRegelTestUtil {

    fun oppfylteDelvilkårPassBarn() = listOf(
        delvilkårDto(VurderingDto(RegelId.HAR_ALDER_LAVERE_ENN_GRENSEVERDI, SvarId.NEI, "en begrunnelse")),
        delvilkårDto(VurderingDto(RegelId.DEKKES_UTGIFTER_ANNET_REGELVERK, SvarId.NEI)),
        delvilkårDto(VurderingDto(RegelId.ANNEN_FORELDER_MOTTAR_STØTTE, SvarId.NEI)),
        delvilkårDto(VurderingDto(RegelId.UTGIFTER_DOKUMENTERT, SvarId.JA)),
    )

    private fun delvilkårDto(vararg vurderinger: VurderingDto) = DelvilkårDto(
        resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
        vurderinger = vurderinger.toList(),
    )
}
