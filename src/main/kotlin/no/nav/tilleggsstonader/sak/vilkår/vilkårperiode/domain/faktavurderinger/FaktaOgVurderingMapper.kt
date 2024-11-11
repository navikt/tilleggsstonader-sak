package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.ResultatEvaluering

fun mapFaktaOgVurderingDto(
    vilkårperiode: LagreVilkårperiode,
    resultatEvaluering: ResultatEvaluering,
): FaktaOgVurdering {
    return when (vilkårperiode.type) {
        AktivitetType.TILTAK -> {
            require(resultatEvaluering.delvilkår is DelvilkårAktivitet)
            TiltakTilsynBarn(
                fakta = FaktaAktivitetTilsynBarn(aktivitetsdager = vilkårperiode.aktivitetsdager!!),
                vurderinger = VurderingTiltakTilsynBarn(lønnet = resultatEvaluering.delvilkår.lønnet),
            )
        }

        AktivitetType.UTDANNING -> UtdanningTilsynBarn(
            fakta = FaktaAktivitetTilsynBarn(aktivitetsdager = vilkårperiode.aktivitetsdager!!),
        )

        AktivitetType.REELL_ARBEIDSSØKER -> ReellArbeidsøkerTilsynBarn(
            fakta = FaktaAktivitetTilsynBarn(aktivitetsdager = vilkårperiode.aktivitetsdager!!),
        )

        AktivitetType.INGEN_AKTIVITET -> {
            feilHvis(vilkårperiode.aktivitetsdager != null) {
                "Kan ikke registrere aktivitetsdager på ingen aktivitet"
            }
            IngenAktivitetTilsynBarn
        }

        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeTilsynBarn
        MålgruppeType.SYKEPENGER_100_PROSENT -> SykepengerTilsynBarn
        MålgruppeType.OMSTILLINGSSTØNAD -> {
            require(resultatEvaluering.delvilkår is DelvilkårMålgruppe)
            OmstillingsstønadTilsynBarn(
                vurderinger = VurderingOmstillingsstønad(medlemskap = resultatEvaluering.delvilkår.medlemskap),
            )
        }

        MålgruppeType.OVERGANGSSTØNAD -> {
            require(resultatEvaluering.delvilkår is DelvilkårMålgruppe)
            require(resultatEvaluering.delvilkår.medlemskap.svar == SvarJaNei.JA_IMPLISITT)
            require(resultatEvaluering.delvilkår.dekketAvAnnetRegelverk.svar == null)
            OvergangssstønadTilsynBarn
        }

        MålgruppeType.AAP -> {
            require(resultatEvaluering.delvilkår is DelvilkårMålgruppe)
            require(resultatEvaluering.delvilkår.medlemskap.svar == SvarJaNei.JA_IMPLISITT)
            AAPTilsynBarn(
                vurderinger = VurderingAAP(dekketAvAnnetRegelverk = resultatEvaluering.delvilkår.dekketAvAnnetRegelverk),
            )
        }

        MålgruppeType.UFØRETRYGD -> {
            require(resultatEvaluering.delvilkår is DelvilkårMålgruppe)
            UføretrygdTilsynBarn(
                vurderinger = VurderingUføretrygd(
                    dekketAvAnnetRegelverk = resultatEvaluering.delvilkår.dekketAvAnnetRegelverk,
                    medlemskap = resultatEvaluering.delvilkår.medlemskap,
                ),
            )
        }
        MålgruppeType.NEDSATT_ARBEIDSEVNE -> {
            require(resultatEvaluering.delvilkår is DelvilkårMålgruppe)
            NedsattArbeidsevneTilsynBarn(
                vurderinger = VurderingNedsattArbeidsevne(
                    dekketAvAnnetRegelverk = resultatEvaluering.delvilkår.dekketAvAnnetRegelverk,
                    medlemskap = resultatEvaluering.delvilkår.medlemskap,
                ),
            )
        }

        MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger")
    }
}
