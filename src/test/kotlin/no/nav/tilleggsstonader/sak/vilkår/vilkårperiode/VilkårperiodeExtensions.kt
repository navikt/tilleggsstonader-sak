package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeEllerAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.ResultatEvaluering

object VilkårperiodeExtensions {
    val MålgruppeEllerAktivitet.medlemskap: DelvilkårVilkårperiode.Vurdering
        get() = (this.delvilkår as DelvilkårMålgruppe).medlemskap
    val MålgruppeEllerAktivitet.dekketAvAnnetRegelverk: DelvilkårVilkårperiode.Vurdering
        get() = (this.delvilkår as DelvilkårMålgruppe).dekketAvAnnetRegelverk

    val MålgruppeEllerAktivitet.lønnet: DelvilkårVilkårperiode.Vurdering
        get() = (this.delvilkår as DelvilkårAktivitet).lønnet

    val VilkårperiodeDto.medlemskap: VurderingDto?
        get() = (this.delvilkår as DelvilkårMålgruppeDto).medlemskap

    val VilkårperiodeDto.dekketAvAnnetRegelverk: VurderingDto?
        get() = (this.delvilkår as DelvilkårMålgruppeDto).dekketAvAnnetRegelverk

    val VilkårperiodeDto.lønnet: VurderingDto?
        get() = (this.delvilkår as DelvilkårAktivitetDto).lønnet

    val ResultatEvaluering.medlemskap: DelvilkårVilkårperiode.Vurdering
        get() = (this.delvilkår as DelvilkårMålgruppe).medlemskap

    val ResultatEvaluering.dekketAvAnnetRegelverk: DelvilkårVilkårperiode.Vurdering
        get() = (this.delvilkår as DelvilkårMålgruppe).dekketAvAnnetRegelverk

    val ResultatEvaluering.lønnet: DelvilkårVilkårperiode.Vurdering
        get() = (this.delvilkår as DelvilkårAktivitet).lønnet
}
