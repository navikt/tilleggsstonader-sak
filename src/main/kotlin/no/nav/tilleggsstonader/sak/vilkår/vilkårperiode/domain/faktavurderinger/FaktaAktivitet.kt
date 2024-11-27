package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

sealed interface FaktaAktivitetsdager : Fakta {
    val aktivitetsdager: Int
}

sealed interface FaktaProsent : Fakta {
    val prosent: Int
}
