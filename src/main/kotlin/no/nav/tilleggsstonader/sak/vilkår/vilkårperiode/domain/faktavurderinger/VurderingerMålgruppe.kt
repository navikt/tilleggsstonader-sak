package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

sealed interface VurderingerMålgruppe : Vurderinger

sealed interface MedlemskapVurdering : VurderingerMålgruppe {
    val medlemskap: Vurdering
}

sealed interface DekketAvAnnetRegelverkVurdering : VurderingerMålgruppe {
    val dekketAvAnnetRegelverk: Vurdering
}

data class VurderingAAP(
    override val dekketAvAnnetRegelverk: Vurdering,
) : MedlemskapVurdering, DekketAvAnnetRegelverkVurdering {
    override val medlemskap: Vurdering = Vurdering.VURDERING_IMPLISITT_OPPFYLT
}

data class VurderingUføretrygd(
    override val medlemskap: Vurdering,
    override val dekketAvAnnetRegelverk: Vurdering,
) : MedlemskapVurdering, DekketAvAnnetRegelverkVurdering

data class VurderingNedsattArbeidsevne(
    override val medlemskap: Vurdering,
    override val dekketAvAnnetRegelverk: Vurdering,
) : MedlemskapVurdering, DekketAvAnnetRegelverkVurdering

data class VurderingOmstillingsstønad(
    override val medlemskap: Vurdering,
) : MedlemskapVurdering

data object VurderingOvergangsstønad : MedlemskapVurdering {
    override val medlemskap: Vurdering =
        Vurdering.VURDERING_IMPLISITT_OPPFYLT
}
