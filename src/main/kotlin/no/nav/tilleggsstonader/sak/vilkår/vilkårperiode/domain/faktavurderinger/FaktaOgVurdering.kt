package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType

/**
 * Felles interface for Fakta og Vurderinger
 */
sealed interface FaktaOgVurdering : FaktaOgVurderingJson {
    val type: TypeFaktaOgVurdering
    val fakta: Fakta
    val vurderinger: Vurderinger
}

sealed interface TypeFaktaOgVurdering {
    val vilkårperiodeType: VilkårperiodeType
}

sealed interface MålgruppeFaktaOgVurdering : FaktaOgVurdering
sealed interface AktivitetFaktaOgVurdering : FaktaOgVurdering

sealed interface Fakta
data object TomFakta : Fakta

sealed interface Vurderinger
data object TomVurdering : Vurderinger
