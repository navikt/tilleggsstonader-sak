package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.DekketAvAnnetRegelverkVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfVurderingOrThrow
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.LønnetVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MedlemskapVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.Vurdering

object VilkårperiodeExtensions {
    val Vilkårperiode.medlemskap: Vurdering
        get() =
            this.faktaOgVurdering.vurderinger
                .takeIfVurderingOrThrow<MedlemskapVurdering>()
                .medlemskap

    val Vilkårperiode.dekketAvAnnetRegelverk: Vurdering
        get() =
            this.faktaOgVurdering.vurderinger
                .takeIfVurderingOrThrow<DekketAvAnnetRegelverkVurdering>()
                .dekketAvAnnetRegelverk

    val Vilkårperiode.lønnet: Vurdering
        get() =
            this.faktaOgVurdering.vurderinger
                .takeIfVurderingOrThrow<LønnetVurdering>()
                .lønnet
}
