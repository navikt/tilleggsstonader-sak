package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

sealed interface VurderingerAktivitet : Vurderinger

sealed interface LønnetVurdering : VurderingerAktivitet {
    val lønnet: VurderingLønnet
}

sealed interface HarUtgifterVurdering : VurderingerAktivitet {
    val harUtgifter: VurderingHarUtgifter
}
