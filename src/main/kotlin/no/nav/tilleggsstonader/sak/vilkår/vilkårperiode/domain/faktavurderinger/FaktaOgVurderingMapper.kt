package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.EvalueringMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.ResultatEvaluering

fun mapFaktaOgVurderingDto(
    vilkårperiode: LagreVilkårperiode,
    resultatEvaluering: ResultatEvaluering,
): FaktaOgVurdering {
    return when (vilkårperiode.type) {
        AktivitetType.TILTAK -> {
            require(resultatEvaluering.delvilkår is DelvilkårAktivitet)
            TiltakTilsynBarn(
                fakta = FaktaAktivitetTilsynBarn(vilkårperiode.aktivitetsdager!!),
                vurderinger = VurderingTiltakTilsynBarn(lønnet = resultatEvaluering.delvilkår.lønnet),
            )
        }

        AktivitetType.UTDANNING -> UtdanningTilsynBarn(
            fakta = FaktaAktivitetTilsynBarn(vilkårperiode.aktivitetsdager!!),
        )

        AktivitetType.REELL_ARBEIDSSØKER -> ReellArbeidsøkerTilsynBarn(
            fakta = FaktaAktivitetTilsynBarn(vilkårperiode.aktivitetsdager!!),
        )

        AktivitetType.INGEN_AKTIVITET -> {
            feilHvis(vilkårperiode.aktivitetsdager != null) {
                "Kan ikke registrere aktivitetsdager på ingen aktivitet"
            }
            IngenAktivitetTilsynBarn
        }

        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeTilsynBarn
        MålgruppeType.SYKEPENGER_100_PROSENT -> SykepengerTilsynBarn

        is MålgruppeType -> {
            require(vilkårperiode.delvilkår is DelvilkårMålgruppeDto)
            val resultatEvaluering = EvalueringMålgruppe.utledResultat(vilkårperiode.type, vilkårperiode.delvilkår)
            require(resultatEvaluering.delvilkår is DelvilkårMålgruppe)
            FellesMålgruppeTilsynBarn(
                type = MålgruppeTilsynBarnType.entries.single { it.vilkårperiodeType == vilkårperiode.type },
                vurderinger = MålgruppeVurderinger(
                    medlemskap = resultatEvaluering.delvilkår.medlemskap,
                    dekketAvAnnetRegelverk = resultatEvaluering.delvilkår.dekketAvAnnetRegelverk,
                ),
            )
        }
    }
}
