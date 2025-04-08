package no.nav.tilleggsstonader.sak.oppfølging.domain

import no.nav.tilleggsstonader.sak.oppfølging.domain.OppfølgingAktivitet.Companion.fraVilkårperioder
import no.nav.tilleggsstonader.sak.oppfølging.domain.OppfølgingMålgruppe.Companion.fraVilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode

data class OppfølgingVilkårperioder(
    val aktiviteter: List<OppfølgingAktivitet>,
    val målgrupper: List<OppfølgingMålgruppe>,
) {
    companion object {
        fun fraVilkårperider(
            vilkårperioder: List<Vilkårperiode>,
            registerAktiviteter: OppfølgingRegisterAktiviteter,
        ): OppfølgingVilkårperioder =
            OppfølgingVilkårperioder(
                målgrupper = fraVilkårperioder(vilkårperioder),
                aktiviteter = fraVilkårperioder(vilkårperioder, registerAktiviteter),
            )
    }
}
