package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId

object BoutgifterRegelTestUtil {
    fun oppfylteDelvilkårMidlertidigOvernatting() =
        listOf(
            delvilkår(Vurdering(RegelId.NØDVENDIGE_MERUTGIFTER, SvarId.JA)),
        )

    fun ikkeOppfylteDelvilkårMidlertidigOvernatting() =
        listOf(
            delvilkår(Vurdering(RegelId.NØDVENDIGE_MERUTGIFTER, SvarId.NEI)),
        )

    fun oppfylteDelvilkårMidlertidigOvernattingDto() = oppfylteDelvilkårMidlertidigOvernatting().map { it.tilDto() }

    private fun delvilkår(vararg vurderinger: Vurdering) =
        Delvilkår(
            resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            vurderinger = vurderinger.toList(),
        )
}
