package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode

sealed interface VurderingerMålgruppe : Vurderinger

sealed interface MedlemskapVurdering : VurderingerMålgruppe {
    val medlemskap: DelvilkårVilkårperiode.Vurdering
}

sealed interface DekketAvAnnetRegelverkVurdering : VurderingerMålgruppe {
    val dekketAvAnnetRegelverk: DelvilkårVilkårperiode.Vurdering
}

data class VurderingAAP(
    override val dekketAvAnnetRegelverk: DelvilkårVilkårperiode.Vurdering,
) : MedlemskapVurdering, DekketAvAnnetRegelverkVurdering {
    override val medlemskap: DelvilkårVilkårperiode.Vurdering =
        DelvilkårVilkårperiode.Vurdering.VURDERING_IMPLISITT_OPPFYLT
}

data class VurderingUføretrygd(
    override val medlemskap: DelvilkårVilkårperiode.Vurdering,
    override val dekketAvAnnetRegelverk: DelvilkårVilkårperiode.Vurdering,
) : MedlemskapVurdering, DekketAvAnnetRegelverkVurdering

data class VurderingNedsattArbeidsevne(
    override val medlemskap: DelvilkårVilkårperiode.Vurdering,
    override val dekketAvAnnetRegelverk: DelvilkårVilkårperiode.Vurdering,
) : MedlemskapVurdering, DekketAvAnnetRegelverkVurdering

data class VurderingOmstillingsstønad(
    override val medlemskap: DelvilkårVilkårperiode.Vurdering,
) : MedlemskapVurdering

data object VurderingOvergangsstønad : MedlemskapVurdering {
    override val medlemskap: DelvilkårVilkårperiode.Vurdering =
        DelvilkårVilkårperiode.Vurdering.VURDERING_IMPLISITT_OPPFYLT
}
