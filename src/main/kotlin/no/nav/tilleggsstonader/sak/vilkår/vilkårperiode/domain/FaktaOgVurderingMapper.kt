package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

fun mapFaktaOgVurdering(vilkårperiode: VilkårperiodeOld<*>): FaktaOgVurdering = with(vilkårperiode) {
    return when (this.type) {
        AktivitetType.TILTAK -> TiltakTilsynBarn(
            fakta = FaktaAktivitetTilsynBarn(this.vilkårOgFakta.aktivitetsdager!!),
            vurderinger = TiltakTilsynBarnVurdering(lønnet = (this.vilkårOgFakta.delvilkår as DelvilkårAktivitet).lønnet),
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
            fom = fom,
            tom = tom,
        )

        is MålgruppeType -> MålgruppeTilsynBarn(
            vurderinger = MålgruppeVurderingerTilsynBarn(
                medlemskap = (this.vilkårOgFakta.delvilkår as DelvilkårMålgruppe).medlemskap,
                dekketAvAnnetRegelverk = this.vilkårOgFakta.delvilkår.dekketAvAnnetRegelverk,
            ),
            fom = fom,
            tom = tom,
            begrunnelse = this.vilkårOgFakta.begrunnelse,
        )
    }
}
