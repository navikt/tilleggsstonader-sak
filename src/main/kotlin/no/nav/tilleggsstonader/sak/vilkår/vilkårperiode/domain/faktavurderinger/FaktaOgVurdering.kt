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
