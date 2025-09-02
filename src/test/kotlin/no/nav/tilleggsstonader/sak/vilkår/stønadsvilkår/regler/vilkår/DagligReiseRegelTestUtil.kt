package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId

object DagligReiseRegelTestUtil {
    fun oppfylteDelvilkårDagligReiseOffentligTransport() =
        listOf(
            delvilkår(
                Vurdering(
                    regelId = RegelId.AVSTAND_OVER_SEKS_KM,
                    svar = SvarId.JA,
                ),
                Vurdering(
                    regelId = RegelId.KAN_BRUKER_REISE_MED_OFFENTLIG_TRANSPORT,
                    svar = SvarId.JA,
                    begrunnelse = "En begrunnelse på delvilkåret",
                ),
            ),
        )

    private fun delvilkår(vararg vurderinger: Vurdering) =
        Delvilkår(
            resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            vurderinger = vurderinger.toList(),
        )
}
