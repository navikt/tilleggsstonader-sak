package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode

sealed interface FaktaOgVurdering : FaktaOgVurderingJson {
    val type: TypeFaktaOgVurdering
    val fakta: Fakta
    val vurderinger: Vurderinger
}

sealed interface MålgruppeFaktaOgVurdering : FaktaOgVurdering
sealed interface AktivitetFaktaOgVurdering : FaktaOgVurdering

data class MålgruppeVurderinger(
    override val medlemskap: DelvilkårVilkårperiode.Vurdering,
    override val dekketAvAnnetRegelverk: DelvilkårVilkårperiode.Vurdering,
) : MedlemskapVurdering, DekketAvAnnetRegelverkVurdering

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

data object TomFakta : Fakta

sealed interface Vurderinger

sealed interface LønnetVurdering : Vurderinger {
    val lønnet: DelvilkårVilkårperiode.Vurdering
}

sealed interface MedlemskapVurdering : Vurderinger {
    val medlemskap: DelvilkårVilkårperiode.Vurdering
}

sealed interface DekketAvAnnetRegelverkVurdering : Vurderinger {
    val dekketAvAnnetRegelverk: DelvilkårVilkårperiode.Vurdering
}

sealed interface Fakta
sealed interface FaktaAktivitetsdager : Fakta {
    val aktivitetsdager: Int
}

data object TomVurdering : Vurderinger
