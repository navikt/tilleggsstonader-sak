package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

sealed interface VurderingerMålgruppe : Vurderinger

sealed interface MedlemskapVurdering : VurderingerMålgruppe {
    val medlemskap: VurderingMedlemskap
}

sealed interface DekketAvAnnetRegelverkVurdering : VurderingerMålgruppe {
    val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk
}

sealed interface AldersvilkårVurdering : VurderingerMålgruppe {
    val aldersvilkår: VurderingAldersVilkår
}

sealed interface MottarSykepengerForFulltidsstillingVurdering : VurderingerMålgruppe {
    val mottarSykepengerForFulltidsstilling: VurderingMottarSykepengerForFulltidsstilling
}

data class VurderingAAP(
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
    override val aldersvilkår: VurderingAldersVilkår,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    AldersvilkårVurdering,
    MottarSykepengerForFulltidsstillingVurdering {
    override val medlemskap: VurderingMedlemskap = VurderingMedlemskap.IMPLISITT
    override val mottarSykepengerForFulltidsstilling: VurderingMottarSykepengerForFulltidsstilling =
        VurderingMottarSykepengerForFulltidsstilling.NEI_IMPLISITT
}

data class VurderingUføretrygd(
    override val medlemskap: VurderingMedlemskap,
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
    override val aldersvilkår: VurderingAldersVilkår,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    AldersvilkårVurdering,
    MottarSykepengerForFulltidsstillingVurdering {
    override val mottarSykepengerForFulltidsstilling: VurderingMottarSykepengerForFulltidsstilling =
        VurderingMottarSykepengerForFulltidsstilling.NEI_IMPLISITT
}

data class VurderingNedsattArbeidsevne(
    override val medlemskap: VurderingMedlemskap,
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
    override val aldersvilkår: VurderingAldersVilkår,
    override val mottarSykepengerForFulltidsstilling: VurderingMottarSykepengerForFulltidsstilling,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    AldersvilkårVurdering,
    MottarSykepengerForFulltidsstillingVurdering

data class VurderingOmstillingsstønad(
    override val medlemskap: VurderingMedlemskap,
    override val aldersvilkår: VurderingAldersVilkår,
) : MedlemskapVurdering,
    AldersvilkårVurdering

data object VurderingOvergangsstønad : MedlemskapVurdering {
    override val medlemskap: VurderingMedlemskap = VurderingMedlemskap.IMPLISITT
}
