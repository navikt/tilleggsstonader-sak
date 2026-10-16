package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId

object PassBarnRegelTestUtil {
    fun oppfylteDelvilkårPassBarn() =
        listOf(
            Delvilkår(resultat = Vilkårsresultat.OPPFYLT, vurderinger = listOf(Vurdering(RegelId.UTGIFTER_DOKUMENTERT, SvarId.JA))),
            Delvilkår(
                resultat = Vilkårsresultat.OPPFYLT,
                vurderinger = listOf(Vurdering(RegelId.ANNEN_FORELDER_MOTTAR_STØTTE, SvarId.NEI)),
            ),
            Delvilkår(
                resultat = Vilkårsresultat.OPPFYLT,
                vurderinger = listOf(Vurdering(RegelId.HAR_FULLFØRT_FJERDEKLASSE, SvarId.NEI, "en begrunnelse")),
            ),
        )

    fun ikkeOppfylteDelvilkårPassBarn() =
        listOf(
            Delvilkår(resultat = Vilkårsresultat.OPPFYLT, vurderinger = listOf(Vurdering(RegelId.UTGIFTER_DOKUMENTERT, SvarId.JA))),
            Delvilkår(
                resultat = Vilkårsresultat.IKKE_OPPFYLT,
                vurderinger = listOf(Vurdering(RegelId.ANNEN_FORELDER_MOTTAR_STØTTE, SvarId.JA, "en begrunnelse")),
            ),
            Delvilkår(
                resultat = Vilkårsresultat.OPPFYLT,
                vurderinger = listOf(Vurdering(RegelId.HAR_FULLFØRT_FJERDEKLASSE, SvarId.NEI, "en begrunnelse")),
            ),
        )

    fun oppfylteDelvilkårPassBarnDto() = oppfylteDelvilkårPassBarn().map { it.tilDto() }

    fun ikkeOppfylteDelvilkårPassBarnDto() = ikkeOppfylteDelvilkårPassBarn().map { it.tilDto() }
}
