@file:Suppress("ktlint:standard:filename") // Kan fjernes når flere vurderinger er lagt til

package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

sealed interface VurderingerAktivitet : Vurderinger

sealed interface LønnetVurdering : VurderingerAktivitet {
    val lønnet: Vurdering
}
