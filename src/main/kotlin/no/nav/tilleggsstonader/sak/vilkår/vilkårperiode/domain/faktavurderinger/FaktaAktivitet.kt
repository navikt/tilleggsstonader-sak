@file:Suppress("ktlint:standard:filename") // Kan fjernes når flere fakta er lagt til

package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

sealed interface FaktaAktivitetsdager : Fakta {
    val aktivitetsdager: Int
}
