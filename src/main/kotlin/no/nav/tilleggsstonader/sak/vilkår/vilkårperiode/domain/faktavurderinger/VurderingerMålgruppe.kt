package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

sealed interface VurderingerMålgruppe : Vurderinger

sealed interface MedlemskapVurdering : VurderingerMålgruppe {
    val medlemskap: VurderingMedlemskap
}

sealed interface DekketAvAnnetRegelverkVurdering : VurderingerMålgruppe {
    val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk
}

data class VurderingAAP(
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
) : MedlemskapVurdering, DekketAvAnnetRegelverkVurdering {
    override val medlemskap: VurderingMedlemskap = VurderingMedlemskap.IMPLISITT
}

data class VurderingUføretrygd(
    override val medlemskap: VurderingMedlemskap,
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
) : MedlemskapVurdering, DekketAvAnnetRegelverkVurdering

data class VurderingNedsattArbeidsevne(
    override val medlemskap: VurderingMedlemskap,
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
) : MedlemskapVurdering, DekketAvAnnetRegelverkVurdering

data class VurderingOmstillingsstønad(
    override val medlemskap: VurderingMedlemskap,
) : MedlemskapVurdering

data object VurderingOvergangsstønad : MedlemskapVurdering {
    override val medlemskap: VurderingMedlemskap = VurderingMedlemskap.IMPLISITT
}
