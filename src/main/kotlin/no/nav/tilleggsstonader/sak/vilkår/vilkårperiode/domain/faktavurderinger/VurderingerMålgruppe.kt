package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import java.time.LocalDateTime

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

data class VurderingAAP(
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
    override val aldersvilkår: VurderingAldersVilkår,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    AldersvilkårVurdering {
    override val medlemskap: VurderingMedlemskap = VurderingMedlemskap.IMPLISITT
}

data class VurderingUføretrygd(
    override val medlemskap: VurderingMedlemskap,
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
    override val aldersvilkår: VurderingAldersVilkår,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    AldersvilkårVurdering

data class VurderingNedsattArbeidsevne(
    override val medlemskap: VurderingMedlemskap,
    override val dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk,
    override val aldersvilkår: VurderingAldersVilkår,
) : MedlemskapVurdering,
    DekketAvAnnetRegelverkVurdering,
    AldersvilkårVurdering

data class VurderingOmstillingsstønad(
    override val medlemskap: VurderingMedlemskap,
    override val aldersvilkår: VurderingAldersVilkår,
) : MedlemskapVurdering,
    AldersvilkårVurdering

data object VurderingOvergangsstønad : MedlemskapVurdering, AldersvilkårVurdering {
    override val medlemskap: VurderingMedlemskap = VurderingMedlemskap.IMPLISITT
    override val aldersvilkår: VurderingAldersVilkår =
        VurderingAldersVilkår(
            SvarJaNei.JA,
            inputFakta = "inputFakta",
            gitHash = "gitHash",
            tidspunktForVurdering = LocalDateTime.of(2025, 1, 1, 0, 0),
        )
}
