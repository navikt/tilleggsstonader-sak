package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå

sealed interface FaktaAktivitetsdager : Fakta {
    val aktivitetsdager: Int
}

sealed interface FaktaProsent : Fakta {
    val prosent: Int
}

sealed interface FaktaStudienivå : Fakta {
    val studienivå: Studienivå?
}
