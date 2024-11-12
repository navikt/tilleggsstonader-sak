package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AAPTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.NedsattArbeidsevneTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OmstillingsstønadTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OvergangssstønadTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ReellArbeidsøkerTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SykepengerTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UføretrygdTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAAP
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingNedsattArbeidsevne
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingOmstillingsstønad
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingUføretrygd
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.ResultatEvaluering

fun mapFaktaOgVurderingDto(
    vilkårperiode: LagreVilkårperiode,
    resultatEvaluering: ResultatEvaluering,
): FaktaOgVurdering {
    return when (vilkårperiode.type) {
        is AktivitetType -> mapAktiviteter(vilkårperiode, resultatEvaluering)
        is MålgruppeType -> mapMålgrupper(vilkårperiode, resultatEvaluering)
    }
}

private fun mapAktiviteter(
    vilkårperiode: LagreVilkårperiode,
    resultatEvaluering: ResultatEvaluering,
): AktivitetTilsynBarn {
    val type = vilkårperiode.type
    require(type is AktivitetType)
    require(resultatEvaluering.delvilkår is DelvilkårAktivitet)
    return when (type) {
        AktivitetType.TILTAK -> {
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
    }
}

private fun mapMålgrupper(
    vilkårperiode: LagreVilkårperiode,
    resultatEvaluering: ResultatEvaluering,
): MålgruppeTilsynBarn {
    val type = vilkårperiode.type
    require(type is MålgruppeType)
    require(resultatEvaluering.delvilkår is DelvilkårMålgruppe)
    return when (type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeTilsynBarn
        MålgruppeType.SYKEPENGER_100_PROSENT -> SykepengerTilsynBarn
        MålgruppeType.OMSTILLINGSSTØNAD -> {
            OmstillingsstønadTilsynBarn(
                vurderinger = VurderingOmstillingsstønad(medlemskap = resultatEvaluering.delvilkår.medlemskap),
            )
        }

        MålgruppeType.OVERGANGSSTØNAD -> {
            require(resultatEvaluering.delvilkår.medlemskap.svar == SvarJaNei.JA_IMPLISITT)
            require(resultatEvaluering.delvilkår.dekketAvAnnetRegelverk.svar == null)
            OvergangssstønadTilsynBarn
        }

        MålgruppeType.AAP -> {
            require(resultatEvaluering.delvilkår.medlemskap.svar == SvarJaNei.JA_IMPLISITT)
            AAPTilsynBarn(
                vurderinger = VurderingAAP(dekketAvAnnetRegelverk = resultatEvaluering.delvilkår.dekketAvAnnetRegelverk),
            )
        }

        MålgruppeType.UFØRETRYGD -> {
            UføretrygdTilsynBarn(
                vurderinger = VurderingUføretrygd(
                    dekketAvAnnetRegelverk = resultatEvaluering.delvilkår.dekketAvAnnetRegelverk,
                    medlemskap = resultatEvaluering.delvilkår.medlemskap,
                ),
            )
        }

        MålgruppeType.NEDSATT_ARBEIDSEVNE -> {
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
