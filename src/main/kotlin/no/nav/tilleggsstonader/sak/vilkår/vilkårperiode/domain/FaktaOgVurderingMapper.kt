package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.aktivitet.FaktaOgVurderingerAktivitetBarnetilsynDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.aktivitet.LagreAktivitet
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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingMedlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingNedsattArbeidsevne
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingOmstillingsstønad
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingUføretrygd
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.målgruppe.LagreMålgruppe

fun mapFaktaOgVurderingDto(
    vilkårperiode: LagreVilkårperiode,
): FaktaOgVurdering {
    return when (vilkårperiode.type) {
        is AktivitetType -> mapAktiviteter(vilkårperiode)
        is MålgruppeType -> mapMålgrupper(vilkårperiode)
    }
}

private fun mapAktiviteter(
    vilkårperiode: LagreVilkårperiode,
): AktivitetTilsynBarn {
    val type = vilkårperiode.type
    require(type is AktivitetType)
    val delvilkår = vilkårperiode.delvilkår
    require(delvilkår is DelvilkårAktivitetDto)
    return when (type) {
        AktivitetType.TILTAK -> {
            TiltakTilsynBarn(
                fakta = FaktaAktivitetTilsynBarn(aktivitetsdager = vilkårperiode.aktivitetsdager!!),
                vurderinger = VurderingTiltakTilsynBarn(lønnet = mapLønnet(delvilkår)),
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

fun mapAktiviteter(stønadstype: Stønadstype, aktivitet: LagreAktivitet): AktivitetTilsynBarn {
    val faktaOgVurderinger = aktivitet.faktaOgVurderinger;
    when (stønadstype) {
        Stønadstype.BARNETILSYN -> {
            require(faktaOgVurderinger is FaktaOgVurderingerAktivitetBarnetilsynDto)
            return mapAktiviteterBarnetilsyn(aktivitet.type, faktaOgVurderinger)

        }

        Stønadstype.LÆREMIDLER -> error("Vi har ikke implementert inngangsvilkår for læremidler enda.")
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

/**
 * TODO valider at man ikke sender inn vurderinger som ikke er aktuelle for gitt type?
 * Men frontend sender hele delvilkår, inkl det som har resultat implisitt
 */
private fun mapMålgrupper(
    vilkårperiode: LagreVilkårperiode,
): MålgruppeTilsynBarn {
    val type = vilkårperiode.type
    require(type is MålgruppeType)
    val delvilkår = vilkårperiode.delvilkår
    require(delvilkår is DelvilkårMålgruppeDto)
    return when (type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeTilsynBarn
        MålgruppeType.SYKEPENGER_100_PROSENT -> SykepengerTilsynBarn
        MålgruppeType.OMSTILLINGSSTØNAD -> {
            OmstillingsstønadTilsynBarn(
                vurderinger = VurderingOmstillingsstønad(
                    medlemskap = mapMedlemskap(delvilkår),
                ),
            )
        }

        MålgruppeType.OVERGANGSSTØNAD -> {
            OvergangssstønadTilsynBarn
        }

        MålgruppeType.AAP -> {
            AAPTilsynBarn(
                vurderinger = VurderingAAP(
                    dekketAvAnnetRegelverk = mapDekketAvAnnetRegelverk(delvilkår),
                ),
            )
        }

        MålgruppeType.UFØRETRYGD -> {
            UføretrygdTilsynBarn(
                vurderinger = VurderingUføretrygd(
                    dekketAvAnnetRegelverk = mapDekketAvAnnetRegelverk(delvilkår),
                    medlemskap = mapMedlemskap(delvilkår),
                ),
            )
        }

        MålgruppeType.NEDSATT_ARBEIDSEVNE -> {
            NedsattArbeidsevneTilsynBarn(
                vurderinger = VurderingNedsattArbeidsevne(
                    dekketAvAnnetRegelverk = mapDekketAvAnnetRegelverk(delvilkår),
                    medlemskap = mapMedlemskap(delvilkår),
                ),
            )
        }

        MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger")
    }
}

fun mapMålgruppe(
    målgruppe: LagreMålgruppe,
): MålgruppeTilsynBarn {
    return when (målgruppe.type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeTilsynBarn
        MålgruppeType.SYKEPENGER_100_PROSENT -> SykepengerTilsynBarn
        MålgruppeType.OMSTILLINGSSTØNAD -> {
            OmstillingsstønadTilsynBarn(
                vurderinger = VurderingOmstillingsstønad(
                    medlemskap = VurderingMedlemskap(målgruppe.svarMedlemskap),
                ),
            )
        }

        MålgruppeType.OVERGANGSSTØNAD -> {
            OvergangssstønadTilsynBarn
        }

        MålgruppeType.AAP -> {
            AAPTilsynBarn(
                vurderinger = VurderingAAP(
                    dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(målgruppe.svarUtgifterDekketAvAnnetRegelverk),
                ),
            )
        }

        MålgruppeType.UFØRETRYGD -> {
            UføretrygdTilsynBarn(
                vurderinger = VurderingUføretrygd(
                    dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(målgruppe.svarUtgifterDekketAvAnnetRegelverk),
                    medlemskap = VurderingMedlemskap(målgruppe.svarMedlemskap),
                ),
            )
        }

        MålgruppeType.NEDSATT_ARBEIDSEVNE -> {
            NedsattArbeidsevneTilsynBarn(
                vurderinger = VurderingNedsattArbeidsevne(
                    dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(målgruppe.svarUtgifterDekketAvAnnetRegelverk),
                    medlemskap = VurderingMedlemskap(målgruppe.svarMedlemskap),
                ),
            )
        }

        MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger")
    }
}

private fun mapLønnet(delvilkår: DelvilkårAktivitetDto) =
    VurderingLønnet(delvilkår.lønnet?.svar)

private fun mapDekketAvAnnetRegelverk(delvilkår: DelvilkårMålgruppeDto) =
    VurderingDekketAvAnnetRegelverk(delvilkår.dekketAvAnnetRegelverk?.svar)

private fun mapMedlemskap(delvilkår: DelvilkårMålgruppeDto) =
    VurderingMedlemskap(delvilkår.medlemskap?.svar)
