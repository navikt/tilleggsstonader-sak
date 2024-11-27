package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AAPLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AAPTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.NedsattArbeidsevneLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.NedsattArbeidsevneTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OmstillingsstønadLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OmstillingsstønadTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OvergangssstønadLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OvergangssstønadTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ReellArbeidsøkerTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SykepengerLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SykepengerTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UføretrygdLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UføretrygdTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAAP
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingHarUtgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingMedlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingNedsattArbeidsevne
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingOmstillingsstønad
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingUføretrygd
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgVurderingerAktivitetBarnetilsynDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgVurderingerAktivitetLæremidlerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgVurderingerMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeNy

fun mapFaktaOgVurderingDto(
    vilkårperiode: LagreVilkårperiodeNy,
    stønadstype: Stønadstype,
): FaktaOgVurdering {
    return when (vilkårperiode.type) {
        is AktivitetType -> mapAktiviteter(stønadstype = stønadstype, aktivitet = vilkårperiode)
        is MålgruppeType -> mapMålgruppe(stønadstype = stønadstype, målgruppe = vilkårperiode)
    }
}

private fun mapAktiviteter(stønadstype: Stønadstype, aktivitet: LagreVilkårperiodeNy): AktivitetFaktaOgVurdering {
    val type = aktivitet.type
    require(type is AktivitetType)

    val faktaOgVurderinger = aktivitet.faktaOgVurderinger
    when (stønadstype) {
        Stønadstype.BARNETILSYN -> {
            require(faktaOgVurderinger is FaktaOgVurderingerAktivitetBarnetilsynDto)
            return mapAktiviteterBarnetilsyn(type, faktaOgVurderinger)
        }

        Stønadstype.LÆREMIDLER -> {
            require(faktaOgVurderinger is FaktaOgVurderingerAktivitetLæremidlerDto)
            return mapAktiviteterLæremidler(type, faktaOgVurderinger)
        }
    }
}

private fun mapMålgruppe(stønadstype: Stønadstype, målgruppe: LagreVilkårperiodeNy): MålgruppeFaktaOgVurdering {
    val type = målgruppe.type
    require(type is MålgruppeType)

    val faktaOgVurderinger = målgruppe.faktaOgVurderinger
    require(faktaOgVurderinger is FaktaOgVurderingerMålgruppeDto)

    when (stønadstype) {
        Stønadstype.BARNETILSYN -> {
            return mapMålgruppeBarnetilsyn(type, faktaOgVurderinger)
        }

        Stønadstype.LÆREMIDLER -> {
            return mapMålgruppeLæremidler(type, faktaOgVurderinger)
        }
    }
}

private fun mapAktiviteterBarnetilsyn(
    aktivitetType: AktivitetType,
    faktaOgVurderinger: FaktaOgVurderingerAktivitetBarnetilsynDto,
): AktivitetTilsynBarn {
    return when (aktivitetType) {
        AktivitetType.TILTAK -> {
            TiltakTilsynBarn(
                fakta = FaktaAktivitetTilsynBarn(aktivitetsdager = faktaOgVurderinger.aktivitetsdager!!),
                vurderinger = VurderingTiltakTilsynBarn(lønnet = VurderingLønnet(faktaOgVurderinger.svarLønnet)),
            )
        }

        AktivitetType.UTDANNING -> UtdanningTilsynBarn(
            fakta = FaktaAktivitetTilsynBarn(aktivitetsdager = faktaOgVurderinger.aktivitetsdager!!),
        )

        AktivitetType.REELL_ARBEIDSSØKER -> ReellArbeidsøkerTilsynBarn(
            fakta = FaktaAktivitetTilsynBarn(aktivitetsdager = faktaOgVurderinger.aktivitetsdager!!),
        )

        AktivitetType.INGEN_AKTIVITET -> {
            feilHvis(faktaOgVurderinger.aktivitetsdager != null) {
                "Kan ikke registrere aktivitetsdager på ingen aktivitet"
            }
            IngenAktivitetTilsynBarn
        }
    }
}

fun mapAktiviteterLæremidler(
    type: AktivitetType,
    faktaOgVurderinger: FaktaOgVurderingerAktivitetLæremidlerDto,
): AktivitetLæremidler {
    return when (type) {
        AktivitetType.TILTAK -> TiltakLæremidler(
            fakta = FaktaAktivitetLæremidler(prosent = faktaOgVurderinger.prosent!!),
            vurderinger = VurderingTiltakLæremidler(harUtgifter = VurderingHarUtgifter(faktaOgVurderinger.svarHarUtgifter)),
        )

        AktivitetType.UTDANNING -> UtdanningLæremidler(
            fakta = FaktaAktivitetLæremidler(prosent = faktaOgVurderinger.prosent!!),
        )

        AktivitetType.INGEN_AKTIVITET -> IngenAktivitetLæremidler

        AktivitetType.REELL_ARBEIDSSØKER -> brukerfeil("Reell arbeidssøker er ikke en gyldig aktivitet for læremidler")
    }
}

private fun mapMålgruppeBarnetilsyn(
    type: MålgruppeType,
    faktaOgVurderinger: FaktaOgVurderingerMålgruppeDto,
): MålgruppeTilsynBarn {
    return when (type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeTilsynBarn
        MålgruppeType.SYKEPENGER_100_PROSENT -> SykepengerTilsynBarn
        MålgruppeType.OMSTILLINGSSTØNAD -> {
            OmstillingsstønadTilsynBarn(
                vurderinger = VurderingOmstillingsstønad(
                    medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                ),
            )
        }

        MålgruppeType.OVERGANGSSTØNAD -> {
            OvergangssstønadTilsynBarn
        }

        MålgruppeType.AAP -> {
            AAPTilsynBarn(
                vurderinger = VurderingAAP(
                    dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                ),
            )
        }

        MålgruppeType.UFØRETRYGD -> {
            UføretrygdTilsynBarn(
                vurderinger = VurderingUføretrygd(
                    dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                    medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                ),
            )
        }

        MålgruppeType.NEDSATT_ARBEIDSEVNE -> {
            NedsattArbeidsevneTilsynBarn(
                vurderinger = VurderingNedsattArbeidsevne(
                    dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                    medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                ),
            )
        }

        MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger")
    }
}

private fun mapMålgruppeLæremidler(
    type: MålgruppeType,
    faktaOgVurderinger: FaktaOgVurderingerMålgruppeDto,
): MålgruppeLæremidler {
    return when (type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeLæremidler
        MålgruppeType.SYKEPENGER_100_PROSENT -> SykepengerLæremidler
        MålgruppeType.OMSTILLINGSSTØNAD -> {
            OmstillingsstønadLæremidler(
                vurderinger = VurderingOmstillingsstønad(
                    medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                ),
            )
        }

        MålgruppeType.OVERGANGSSTØNAD -> {
            OvergangssstønadLæremidler
        }

        MålgruppeType.AAP -> {
            AAPLæremidler(
                vurderinger = VurderingAAP(
                    dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                ),
            )
        }

        MålgruppeType.UFØRETRYGD -> {
            UføretrygdLæremidler(
                vurderinger = VurderingUføretrygd(
                    dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                    medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                ),
            )
        }

        MålgruppeType.NEDSATT_ARBEIDSEVNE -> {
            NedsattArbeidsevneLæremidler(
                vurderinger = VurderingNedsattArbeidsevne(
                    dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                    medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                ),
            )
        }

        MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger")
    }
}
