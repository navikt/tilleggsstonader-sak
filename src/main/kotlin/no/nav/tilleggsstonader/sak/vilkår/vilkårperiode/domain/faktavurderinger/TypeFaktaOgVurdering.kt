package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType

sealed interface TypeFaktaOgVurdering {
    val vilkårperiodeType: VilkårperiodeType
}
