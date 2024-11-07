package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeOld

fun mapFaktaOgVurdering(vilkårperiode: VilkårperiodeOld<*>): FaktaOgVurdering = with(vilkårperiode) {
    return when (this.type) {
        AktivitetType.TILTAK -> TiltakTilsynBarn(
            fakta = FaktaAktivitetTilsynBarn(this.vilkårOgFakta.aktivitetsdager!!),
            vurderinger = VurderingTiltakTilsynBarn(lønnet = (this.vilkårOgFakta.delvilkår as DelvilkårAktivitet).lønnet),
            fom = fom,
            tom = tom,
            begrunnelse = this.vilkårOgFakta.begrunnelse,
        )

        AktivitetType.UTDANNING -> UtdanningTilsynBarn(
            fakta = FaktaAktivitetTilsynBarn(this.vilkårOgFakta.aktivitetsdager!!),
            fom = fom,
            tom = tom,
            begrunnelse = this.vilkårOgFakta.begrunnelse,
        )

        AktivitetType.REELL_ARBEIDSSØKER -> ReellArbeidsøkerTilsynBarn(
            fakta = FaktaAktivitetTilsynBarn(this.vilkårOgFakta.aktivitetsdager!!),
            fom = fom,
            tom = tom,
            begrunnelse = this.vilkårOgFakta.begrunnelse,

        )

        AktivitetType.INGEN_AKTIVITET -> TomFaktaOgVurdering(
            type = AktivitetTilsynBarnType.INGEN_AKTIVITET_TILSYN_BARN,
            fom = fom,
            tom = tom,
        )

        is MålgruppeType -> MålgruppeTilsynBarn(
            type = MålgruppeTilsynBarnType.entries.single { it.målgruppeType == this.type },
            vurderinger = MålgruppeVurderinger(
                medlemskap = (this.vilkårOgFakta.delvilkår as DelvilkårMålgruppe).medlemskap,
                dekketAvAnnetRegelverk = this.vilkårOgFakta.delvilkår.dekketAvAnnetRegelverk,
            ),
            fom = fom,
            tom = tom,
            begrunnelse = this.vilkårOgFakta.begrunnelse,
        )
    }
}
