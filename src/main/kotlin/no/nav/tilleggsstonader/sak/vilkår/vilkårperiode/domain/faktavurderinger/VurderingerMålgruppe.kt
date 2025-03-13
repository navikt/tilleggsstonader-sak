package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

sealed interface VurderingerMålgruppe : Vurderinger

sealed interface MedlemskapVurdering : VurderingerMålgruppe {
    val medlemskap: VurderingMedlemskap
}

sealed interface DekketAvAnnetRegelverkVurdering : VurderingerMålgruppe {
    val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk
}

sealed interface MottarSykepengerForFulltidsstillingVurdering : VurderingerMålgruppe {
    val mottarSykepengerForFulltidsstilling: VurderingMottarSykepengerForFulltidsstilling
}

data class VurderingAAP(
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    MottarSykepengerForFulltidsstillingVurdering {
    override val medlemskap: VurderingMedlemskap = VurderingMedlemskap.IMPLISITT
    override val mottarSykepengerForFulltidsstilling: VurderingMottarSykepengerForFulltidsstilling =
        VurderingMottarSykepengerForFulltidsstilling.NEI_IMPLISITT
}

data class VurderingUføretrygd(
    override val medlemskap: VurderingMedlemskap,
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    MottarSykepengerForFulltidsstillingVurdering {
    override val mottarSykepengerForFulltidsstilling: VurderingMottarSykepengerForFulltidsstilling =
        VurderingMottarSykepengerForFulltidsstilling.NEI_IMPLISITT
}

data class VurderingNedsattArbeidsevne(
    override val medlemskap: VurderingMedlemskap,
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
    override val mottarSykepengerForFulltidsstilling: VurderingMottarSykepengerForFulltidsstilling,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    MottarSykepengerForFulltidsstillingVurdering

data class VurderingOmstillingsstønad(
    override val medlemskap: VurderingMedlemskap,
) : MedlemskapVurdering

data object VurderingOvergangsstønad : MedlemskapVurdering {
    override val medlemskap: VurderingMedlemskap = VurderingMedlemskap.IMPLISITT
}
