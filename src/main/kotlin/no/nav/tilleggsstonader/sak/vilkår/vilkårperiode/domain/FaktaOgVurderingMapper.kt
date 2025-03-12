package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlagsdata
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.AldersvilkårVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.AldersvilkårVurdering.vurderAldersvilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AAPBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AAPLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AAPTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenAktivitetBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.NedsattArbeidsevneBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.NedsattArbeidsevneLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.NedsattArbeidsevneTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OmstillingsstønadBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OmstillingsstønadLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OmstillingsstønadTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OvergangssstønadBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OvergangssstønadLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OvergangssstønadTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ReellArbeidsøkerTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SykepengerLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SykepengerTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UføretrygdBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UføretrygdLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UføretrygdTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAAP
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAldersVilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingHarRettTilUtstyrsstipend
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingHarUtgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingMedlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingNedsattArbeidsevne
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingOmstillingsstønad
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingUføretrygd
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingerUtdanningLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetBarnetilsynDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetBoutgifterDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetLæremidlerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode

fun mapFaktaOgSvarDto(
    vilkårperiode: LagreVilkårperiode,
    stønadstype: Stønadstype,
    grunnlagsData: Grunnlagsdata,
): FaktaOgVurdering =
    when (vilkårperiode.type) {
        is AktivitetType -> mapAktiviteter(stønadstype = stønadstype, aktivitet = vilkårperiode)
        is MålgruppeType ->
            mapMålgruppe(
                stønadstype = stønadstype,
                målgruppe = vilkårperiode,
                grunnlagsData = grunnlagsData,
            )
    }

private fun mapAktiviteter(
    stønadstype: Stønadstype,
    aktivitet: LagreVilkårperiode,
): AktivitetFaktaOgVurdering {
    val type = aktivitet.type
    require(type is AktivitetType)

    val faktaOgSvar = aktivitet.faktaOgSvar
    when (stønadstype) {
        Stønadstype.BARNETILSYN -> {
            require(faktaOgSvar is FaktaOgSvarAktivitetBarnetilsynDto)
            return mapAktiviteterBarnetilsyn(type, faktaOgSvar)
        }

        Stønadstype.LÆREMIDLER -> {
            require(faktaOgSvar is FaktaOgSvarAktivitetLæremidlerDto)
            return mapAktiviteterLæremidler(type, faktaOgSvar)
        }

        Stønadstype.BOUTGIFTER -> {
            require(faktaOgSvar is FaktaOgSvarAktivitetBoutgifterDto)
            return mapAktiviteterBoutgifter(type, faktaOgSvar)
        }
    }
}

private fun mapMålgruppe(
    stønadstype: Stønadstype,
    målgruppe: LagreVilkårperiode,
    grunnlagsData: Grunnlagsdata,
): MålgruppeFaktaOgVurdering {
    val type = målgruppe.type
    require(type is MålgruppeType)

    val faktaOgSvar = målgruppe.faktaOgSvar
    require(faktaOgSvar is FaktaOgSvarMålgruppeDto)

    return when (stønadstype) {
        Stønadstype.BARNETILSYN -> {
            mapMålgruppeBarnetilsyn(type, faktaOgSvar, målgruppe, grunnlagsData)
        }

        Stønadstype.LÆREMIDLER -> {
            mapMålgruppeLæremidler(type, faktaOgSvar, målgruppe, grunnlagsData)
        }
        Stønadstype.BOUTGIFTER -> {
            mapMålgruppeBoutgfiter(type, faktaOgSvar, målgruppe, grunnlagsData)
        }
    }
}

private fun mapAktiviteterBarnetilsyn(
    aktivitetType: AktivitetType,
    faktaOgSvar: FaktaOgSvarAktivitetBarnetilsynDto,
): AktivitetTilsynBarn =
    when (aktivitetType) {
        AktivitetType.TILTAK -> {
            TiltakTilsynBarn(
                fakta = FaktaAktivitetTilsynBarn(aktivitetsdager = faktaOgSvar.aktivitetsdager!!),
                vurderinger = VurderingTiltakTilsynBarn(lønnet = VurderingLønnet(faktaOgSvar.svarLønnet)),
            )
        }

        AktivitetType.UTDANNING ->
            UtdanningTilsynBarn(
                fakta = FaktaAktivitetTilsynBarn(aktivitetsdager = faktaOgSvar.aktivitetsdager!!),
            )

        AktivitetType.REELL_ARBEIDSSØKER ->
            ReellArbeidsøkerTilsynBarn(
                fakta = FaktaAktivitetTilsynBarn(aktivitetsdager = faktaOgSvar.aktivitetsdager!!),
            )

        AktivitetType.INGEN_AKTIVITET -> {
            feilHvis(faktaOgSvar.aktivitetsdager != null) {
                "Kan ikke registrere aktivitetsdager på ingen aktivitet"
            }
            IngenAktivitetTilsynBarn
        }
    }

fun mapAktiviteterLæremidler(
    type: AktivitetType,
    faktaOgSvar: FaktaOgSvarAktivitetLæremidlerDto,
): AktivitetLæremidler =
    when (type) {
        AktivitetType.TILTAK ->
            TiltakLæremidler(
                fakta =
                    FaktaAktivitetLæremidler(
                        prosent = faktaOgSvar.prosent!!,
                        studienivå = faktaOgSvar.studienivå,
                    ),
                vurderinger =
                    VurderingTiltakLæremidler(
                        harUtgifter = VurderingHarUtgifter(faktaOgSvar.svarHarUtgifter),
                        harRettTilUtstyrsstipend = VurderingHarRettTilUtstyrsstipend(faktaOgSvar.svarHarRettTilUtstyrsstipend),
                    ),
            )

        AktivitetType.UTDANNING ->
            UtdanningLæremidler(
                fakta =
                    FaktaAktivitetLæremidler(
                        prosent = faktaOgSvar.prosent!!,
                        studienivå = faktaOgSvar.studienivå,
                    ),
                vurderinger =
                    VurderingerUtdanningLæremidler(
                        harUtgifter = VurderingHarUtgifter(faktaOgSvar.svarHarUtgifter),
                        harRettTilUtstyrsstipend = VurderingHarRettTilUtstyrsstipend(faktaOgSvar.svarHarRettTilUtstyrsstipend),
                    ),
            )

        AktivitetType.INGEN_AKTIVITET -> IngenAktivitetLæremidler

        AktivitetType.REELL_ARBEIDSSØKER -> feil("Reell arbeidssøker er ikke en gyldig aktivitet for læremidler")
    }

private fun mapAktiviteterBoutgifter(
    aktivitetType: AktivitetType,
    faktaOgSvar: FaktaOgSvarAktivitetBoutgifterDto,
): AktivitetBoutgifter =
    when (aktivitetType) {
        AktivitetType.TILTAK -> {
            TiltakBoutgifter(
                vurderinger = VurderingTiltakBoutgifter(lønnet = VurderingLønnet(faktaOgSvar.svarLønnet)),
            )
        }

        AktivitetType.UTDANNING -> UtdanningBoutgifter

        AktivitetType.INGEN_AKTIVITET -> IngenAktivitetBoutgifter

        AktivitetType.REELL_ARBEIDSSØKER -> feil("Reell arbeidssøker er ikke en gyldig aktivitet for boutgifter")
    }

private fun lagVurderingAldersvilkår(
    målgruppe: LagreVilkårperiode,
    grunnlagsData: Grunnlagsdata,
): VurderingAldersVilkår =
    VurderingAldersVilkår(
        vurderAldersvilkår(målgruppe, grunnlagsData),
        AldersvilkårVurdering
            .VurderingFaktaEtterlevelseAldersvilkår(
                fødselsdato = grunnlagsData.grunnlag.fødsel?.fødselsdato,
            ),
    )

private fun mapMålgruppeBarnetilsyn(
    type: MålgruppeType,
    faktaOgVurderinger: FaktaOgSvarMålgruppeDto,
    målgruppe: LagreVilkårperiode,
    grunnlagsData: Grunnlagsdata,
): MålgruppeTilsynBarn =
    when (type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeTilsynBarn
        MålgruppeType.SYKEPENGER_100_PROSENT -> SykepengerTilsynBarn
        MålgruppeType.OMSTILLINGSSTØNAD -> {
            OmstillingsstønadTilsynBarn(
                vurderinger =
                    VurderingOmstillingsstønad(
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår =
                            lagVurderingAldersvilkår(målgruppe, grunnlagsData),
                    ),
            )
        }

        MålgruppeType.OVERGANGSSTØNAD -> {
            OvergangssstønadTilsynBarn
        }

        MålgruppeType.AAP -> {
            AAPTilsynBarn(
                vurderinger =
                    VurderingAAP(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        aldersvilkår =
                            lagVurderingAldersvilkår(målgruppe, grunnlagsData),
                    ),
            )
        }

        MålgruppeType.UFØRETRYGD -> {
            UføretrygdTilsynBarn(
                vurderinger =
                    VurderingUføretrygd(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår =
                            lagVurderingAldersvilkår(målgruppe, grunnlagsData),
                    ),
            )
        }

        MålgruppeType.NEDSATT_ARBEIDSEVNE -> {
            NedsattArbeidsevneTilsynBarn(
                vurderinger =
                    VurderingNedsattArbeidsevne(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår =
                            lagVurderingAldersvilkår(målgruppe, grunnlagsData),
                    ),
            )
        }

        MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger")
    }

private fun mapMålgruppeLæremidler(
    type: MålgruppeType,
    faktaOgVurderinger: FaktaOgSvarMålgruppeDto,
    målgruppe: LagreVilkårperiode,
    grunnlagsData: Grunnlagsdata,
): MålgruppeLæremidler =
    when (type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeLæremidler
        MålgruppeType.SYKEPENGER_100_PROSENT -> SykepengerLæremidler
        MålgruppeType.OMSTILLINGSSTØNAD -> {
            OmstillingsstønadLæremidler(
                vurderinger =
                    VurderingOmstillingsstønad(
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår =
                            lagVurderingAldersvilkår(målgruppe, grunnlagsData),
                    ),
            )
        }

        MålgruppeType.OVERGANGSSTØNAD -> {
            OvergangssstønadLæremidler
        }

        MålgruppeType.AAP -> {
            AAPLæremidler(
                vurderinger =
                    VurderingAAP(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        aldersvilkår =
                            lagVurderingAldersvilkår(målgruppe, grunnlagsData),
                    ),
            )
        }

        MålgruppeType.UFØRETRYGD -> {
            UføretrygdLæremidler(
                vurderinger =
                    VurderingUføretrygd(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår =
                            lagVurderingAldersvilkår(målgruppe, grunnlagsData),
                    ),
            )
        }

        MålgruppeType.NEDSATT_ARBEIDSEVNE -> {
            NedsattArbeidsevneLæremidler(
                vurderinger =
                    VurderingNedsattArbeidsevne(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår =
                            lagVurderingAldersvilkår(målgruppe, grunnlagsData),
                    ),
            )
        }

        MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger")
    }

private fun mapMålgruppeBoutgfiter(
    type: MålgruppeType,
    faktaOgVurderinger: FaktaOgSvarMålgruppeDto,
    målgruppe: LagreVilkårperiode,
    grunnlagsData: Grunnlagsdata,
): MålgruppeBoutgifter =
    when (type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeBoutgifter
        MålgruppeType.OMSTILLINGSSTØNAD -> {
            OmstillingsstønadBoutgifter(
                vurderinger =
                    VurderingOmstillingsstønad(
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår =
                            lagVurderingAldersvilkår(målgruppe, grunnlagsData),
                    ),
            )
        }

        MålgruppeType.OVERGANGSSTØNAD -> {
            OvergangssstønadBoutgifter
        }

        MålgruppeType.AAP -> {
            AAPBoutgifter(
                vurderinger =
                    VurderingAAP(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        aldersvilkår =
                            lagVurderingAldersvilkår(målgruppe, grunnlagsData),
                    ),
            )
        }

        MålgruppeType.UFØRETRYGD -> {
            UføretrygdBoutgifter(
                vurderinger =
                    VurderingUføretrygd(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår =
                            lagVurderingAldersvilkår(målgruppe, grunnlagsData),
                    ),
            )
        }

        MålgruppeType.NEDSATT_ARBEIDSEVNE -> {
            NedsattArbeidsevneBoutgifter(
                vurderinger =
                    VurderingNedsattArbeidsevne(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår =
                            lagVurderingAldersvilkår(målgruppe, grunnlagsData),
                    ),
            )
        }

        MålgruppeType.SYKEPENGER_100_PROSENT -> error("Støtter ikke sykepenger for boutgifter")
        MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger")
    }
