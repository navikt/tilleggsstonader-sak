package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode

fun mapFaktaOgVurdering(vilkårperiode: Vilkårperiode): FaktaOgVurdering = with(vilkårperiode) {
    return when (this.type) {
        AktivitetType.TILTAK -> TiltakTilsynBarn(
            fakta = FaktaAktivitetTilsynBarn(this.vilkårOgFakta.aktivitetsdager!!),
            vurderinger = VurderingTiltakTilsynBarn(lønnet = (this.vilkårOgFakta.delvilkår as DelvilkårAktivitet).lønnet),
        )

        AktivitetType.UTDANNING -> UtdanningTilsynBarn(
            fakta = FaktaAktivitetTilsynBarn(this.vilkårOgFakta.aktivitetsdager!!),
        )

        AktivitetType.REELL_ARBEIDSSØKER -> ReellArbeidsøkerTilsynBarn(
            fakta = FaktaAktivitetTilsynBarn(this.vilkårOgFakta.aktivitetsdager!!),
        )

        AktivitetType.INGEN_AKTIVITET -> TomFaktaOgVurdering(
            type = AktivitetTilsynBarnType.INGEN_AKTIVITET_TILSYN_BARN,
        )

        is MålgruppeType -> MålgruppeTilsynBarn(
            type = MålgruppeTilsynBarnType.entries.single { it.målgruppeType == this.type },
            vurderinger = MålgruppeVurderinger(
                medlemskap = (this.vilkårOgFakta.delvilkår as DelvilkårMålgruppe).medlemskap,
                dekketAvAnnetRegelverk = this.vilkårOgFakta.delvilkår.dekketAvAnnetRegelverk,
            ),
        )
    }
}
