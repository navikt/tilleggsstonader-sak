package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.ResultatEvaluering

object VilkårperiodeExtensions {
    val Vilkårperiode.medlemskap: DelvilkårVilkårperiode.Vurdering
        get() = (this.delvilkår as DelvilkårMålgruppe).medlemskap

    val Vilkårperiode.lønnet: DelvilkårVilkårperiode.Vurdering
        get() = (this.delvilkår as DelvilkårAktivitet).lønnet

    val Vilkårperiode.mottarSykepenger: DelvilkårVilkårperiode.Vurdering
        get() = (this.delvilkår as DelvilkårAktivitet).mottarSykepenger

    val ResultatEvaluering.medlemskap: DelvilkårVilkårperiode.Vurdering
        get() = (this.delvilkår as DelvilkårMålgruppe).medlemskap

    val ResultatEvaluering.lønnet: DelvilkårVilkårperiode.Vurdering
        get() = (this.delvilkår as DelvilkårAktivitet).lønnet

    val ResultatEvaluering.mottarSykepenger: DelvilkårVilkårperiode.Vurdering
        get() = (this.delvilkår as DelvilkårAktivitet).mottarSykepenger
}
