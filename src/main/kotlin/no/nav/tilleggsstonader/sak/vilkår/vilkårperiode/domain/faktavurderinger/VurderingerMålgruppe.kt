package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

sealed interface VurderingerMålgruppe : Vurderinger

sealed interface MedlemskapVurdering : VurderingerMålgruppe {
    val medlemskap: VurderingMedlemskap
}

sealed interface DekketAvAnnetRegelverkVurdering : VurderingerMålgruppe {
    val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk
}

sealed interface AldersvilkårOppfyltVurdering : VurderingerMålgruppe {
    val aldersvilkårOppfyltVurdering: VurderingAldersVilkårOppfylt
}

data class VurderingAAP(
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
    override val aldersvilkårOppfyltVurdering: VurderingAldersVilkårOppfylt,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    AldersvilkårOppfyltVurdering {
    override val medlemskap: VurderingMedlemskap = VurderingMedlemskap.IMPLISITT
}

data class VurderingUføretrygd(
    override val medlemskap: VurderingMedlemskap,
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
    override val aldersvilkårOppfyltVurdering: VurderingAldersVilkårOppfylt,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    AldersvilkårOppfyltVurdering

data class VurderingNedsattArbeidsevne(
    override val medlemskap: VurderingMedlemskap,
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
    override val aldersvilkårOppfyltVurdering: VurderingAldersVilkårOppfylt,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    AldersvilkårOppfyltVurdering

data class VurderingOmstillingsstønad(
    override val medlemskap: VurderingMedlemskap,
    override val aldersvilkårOppfyltVurdering: VurderingAldersVilkårOppfylt,
) : MedlemskapVurdering,
    AldersvilkårOppfyltVurdering

data object VurderingOvergangsstønad : MedlemskapVurdering, AldersvilkårOppfyltVurdering {
    override val medlemskap: VurderingMedlemskap = VurderingMedlemskap.IMPLISITT
    override val aldersvilkårOppfyltVurdering: VurderingAldersVilkårOppfylt = VurderingAldersVilkårOppfylt(SvarJaNei.JA_IMPLISITT)
}
