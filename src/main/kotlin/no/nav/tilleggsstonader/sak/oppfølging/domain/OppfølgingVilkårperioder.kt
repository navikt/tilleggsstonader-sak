package no.nav.tilleggsstonader.sak.oppfølging.domain

import no.nav.tilleggsstonader.sak.oppfølging.domain.OppfølgingInngangsvilkårAktivitet.Companion.fraVilkårperioder
import no.nav.tilleggsstonader.sak.oppfølging.domain.OppfølgingInngangsvilkårMålgruppe.Companion.fraVilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode

data class OppfølgingVilkårperioder(
    val aktiviteter: List<OppfølgingInngangsvilkårAktivitet>,
    val målgrupper: List<OppfølgingInngangsvilkårMålgruppe>,
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
