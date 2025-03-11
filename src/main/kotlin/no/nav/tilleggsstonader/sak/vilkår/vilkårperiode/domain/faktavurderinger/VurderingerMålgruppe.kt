package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

sealed interface VurderingerMålgruppe : Vurderinger

sealed interface MedlemskapVurdering : VurderingerMålgruppe {
    val medlemskap: VurderingMedlemskap
}

sealed interface DekketAvAnnetRegelverkVurdering : VurderingerMålgruppe {
    val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk
}

sealed interface MottarFulleSykepengerVurdering : VurderingerMålgruppe {
    val mottarFulleSykepenger: VurderingMottarFulleSykepenger
}

data class VurderingAAP(
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    MottarFulleSykepengerVurdering {
    override val medlemskap: VurderingMedlemskap = VurderingMedlemskap.IMPLISITT
    override val mottarFulleSykepenger: VurderingMottarFulleSykepenger = VurderingMottarFulleSykepenger.NEI_IMPLISITT
}

data class VurderingUføretrygd(
    override val medlemskap: VurderingMedlemskap,
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    MottarFulleSykepengerVurdering {
    override val mottarFulleSykepenger: VurderingMottarFulleSykepenger = VurderingMottarFulleSykepenger.NEI_IMPLISITT
}

data class VurderingNedsattArbeidsevne(
    override val medlemskap: VurderingMedlemskap,
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
    override val mottarFulleSykepenger: VurderingMottarFulleSykepenger,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    MottarFulleSykepengerVurdering

data class VurderingOmstillingsstønad(
    override val medlemskap: VurderingMedlemskap,
) : MedlemskapVurdering

data object VurderingOvergangsstønad : MedlemskapVurdering {
    override val medlemskap: VurderingMedlemskap = VurderingMedlemskap.IMPLISITT
}
