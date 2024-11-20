package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.DekketAvAnnetRegelverkVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfVurderingOrThrow
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.LønnetVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MedlemskapVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VurderingDto

object VilkårperiodeExtensions {

    val Vilkårperiode.medlemskap: Vurdering
        get() = this.faktaOgVurdering.vurderinger
            .takeIfVurderingOrThrow<MedlemskapVurdering>().medlemskap
    val Vilkårperiode.dekketAvAnnetRegelverk: Vurdering
        get() = this.faktaOgVurdering.vurderinger
            .takeIfVurderingOrThrow<DekketAvAnnetRegelverkVurdering>().dekketAvAnnetRegelverk

    val Vilkårperiode.lønnet: Vurdering
        get() = this.faktaOgVurdering.vurderinger
            .takeIfVurderingOrThrow<LønnetVurdering>().lønnet

    val VilkårperiodeDto.medlemskap: VurderingDto?
        get() = (this.delvilkår as DelvilkårMålgruppeDto).medlemskap

    val VilkårperiodeDto.dekketAvAnnetRegelverk: VurderingDto?
        get() = (this.delvilkår as DelvilkårMålgruppeDto).dekketAvAnnetRegelverk

    val VilkårperiodeDto.lønnet: VurderingDto?
        get() = (this.delvilkår as DelvilkårAktivitetDto).lønnet
}
