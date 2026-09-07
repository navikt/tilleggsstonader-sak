package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FødselFaktaGrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.AldersvilkårVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.AldersvilkårVurdering.vurderAldersvilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AAPBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AAPDagligReiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AAPLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AAPPassAvBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AAPReiseOppstartAvslutningHjemreiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AAPReiseTilSamlingTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetDagligReiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetDagligReiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetPassAvBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetReiseOppstartAvslutningHjemreiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetReiseOppstartAvslutningHjemreiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetReiseTilSamlingTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetReiseTilSamlingTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.DagpengerDagligReiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.DagpengerReiseOppstartAvslutningHjemreiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.DagpengerReiseTilSamlingTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetDagligReiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetDagligReiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetPassAvBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenAktivitetBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenAktivitetDagligReiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenAktivitetDagligReiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenAktivitetPassAvBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenAktivitetReiseOppstartAvslutningHjemreiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenAktivitetReiseOppstartAvslutningHjemreiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenAktivitetReiseTilSamlingTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenAktivitetReiseTilSamlingTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeDagligReiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeDagligReiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppePassAvBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeReiseOppstartAvslutningHjemreiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeReiseOppstartAvslutningHjemreiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeReiseTilSamlingTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeReiseTilSamlingTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.InnsattIFengselDagligReiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.InnsattIFengselReiseOppstartAvslutningHjemreiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.InnsattIFengselReiseTilSamlingTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.KvalifiseringsstønadDagligReiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.KvalifiseringsstønadReiseOppstartAvslutningHjemreiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.KvalifiseringsstønadReiseTilSamlingTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeDagligReiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeDagligReiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppePassAvBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeReiseOppstartAvslutningHjemreiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeReiseOppstartAvslutningHjemreiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeReiseTilSamlingTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeReiseTilSamlingTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.NedsattArbeidsevneBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.NedsattArbeidsevneDagligReiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.NedsattArbeidsevneLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.NedsattArbeidsevnePassAvBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.NedsattArbeidsevneReiseOppstartAvslutningHjemreiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.NedsattArbeidsevneReiseTilSamlingTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OmstillingsstønadBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OmstillingsstønadDagligReiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OmstillingsstønadLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OmstillingsstønadPassAvBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OmstillingsstønadReiseOppstartAvslutningHjemreiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OmstillingsstønadReiseTilSamlingTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OvergangssstønadBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OvergangssstønadDagligReiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OvergangssstønadLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OvergangssstønadPassAvBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OvergangssstønadReiseOppstartAvslutningHjemreiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OvergangssstønadReiseTilSamlingTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ReellArbeidsøkerPassAvBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SykepengerLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SykepengerPassAvBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakDagligReiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakDagligReiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakPassAvBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakReiseOppstartAvslutningHjemreiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakReiseOppstartAvslutningHjemreiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakReiseTilSamlingTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakReiseTilSamlingTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakspengerDagligReiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakspengerReiseOppstartAvslutningHjemreiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakspengerReiseTilSamlingTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UføretrygdBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UføretrygdDagligReiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UføretrygdLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UføretrygdPassAvBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UføretrygdReiseOppstartAvslutningHjemreiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UføretrygdReiseTilSamlingTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningDagligReiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningPassAvBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningReiseOppstartAvslutningHjemreiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningReiseTilSamlingTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningReiseTilSamlingTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAAP
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAAPLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAldersVilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingErAktivitetenObligatorisk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingHarRettTilUtstyrsstipend
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingHarUtgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingMedlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingMottarSykepengerForFulltidsstilling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingNedsattArbeidsevne
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingNedsattArbeidsevneLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingOmstillingsstønad
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakDagligReiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakDagligReiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakPassAvBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakReiseOppstartAvslutningHjemreiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakReiseOppstartAvslutningHjemreiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakReiseTilSamlingTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingUføretrygd
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingUføretrygdLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingUtdanningDagligReiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingUtdanningReiseOppstartAvslutningHjemreiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingerUtdanningLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetBoutgifterDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsoDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsrDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetLæremidlerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetPassAvBarnDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetReiseOppstartAvslutningHjemreiseTsoDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetReiseOppstartAvslutningHjemreiseTsrDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetReiseTilSamlingTsoDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetReiseTilSamlingTsrDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode

fun mapFaktaOgSvarDto(
    vilkårperiode: LagreVilkårperiode,
    stønadstype: Stønadstype,
    fødselFaktaGrunnlag: FødselFaktaGrunnlag?,
): FaktaOgVurdering =
    when (vilkårperiode.type) {
        is AktivitetType -> mapAktiviteter(stønadstype = stønadstype, aktivitet = vilkårperiode)
        is MålgruppeType ->
            mapMålgruppe(
                stønadstype = stønadstype,
                målgruppe = vilkårperiode,
                fødselFaktaGrunnlag = fødselFaktaGrunnlag,
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
            require(faktaOgSvar is FaktaOgSvarAktivitetPassAvBarnDto)
            return mapAktiviteterPassAvBarn(type, faktaOgSvar)
        }

        Stønadstype.LÆREMIDLER -> {
            require(faktaOgSvar is FaktaOgSvarAktivitetLæremidlerDto)
            return mapAktiviteterLæremidler(type, faktaOgSvar)
        }

        Stønadstype.BOUTGIFTER -> {
            require(faktaOgSvar is FaktaOgSvarAktivitetBoutgifterDto)
            return mapAktiviteterBoutgifter(type, faktaOgSvar)
        }

        Stønadstype.DAGLIG_REISE_TSO -> {
            require(faktaOgSvar is FaktaOgSvarAktivitetDagligReiseTsoDto)
            return mapAktiviteterDagligReiseTso(type, faktaOgSvar)
        }

        Stønadstype.DAGLIG_REISE_TSR -> {
            require(faktaOgSvar is FaktaOgSvarAktivitetDagligReiseTsrDto)
            return mapAktiviteterDagligReiseTsr(type, faktaOgSvar)
        }

        Stønadstype.REISE_TIL_SAMLING_TSO -> {
            require(faktaOgSvar is FaktaOgSvarAktivitetReiseTilSamlingTsoDto)
            return mapAktiviteterReiseTilSamlingTso(type, faktaOgSvar)
        }

        Stønadstype.REISE_TIL_SAMLING_TSR -> {
            require(faktaOgSvar is FaktaOgSvarAktivitetReiseTilSamlingTsrDto)
            return mapAktiviteterReiseTilSamlingTsr(type)
        }

        Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO -> {
            require(faktaOgSvar is FaktaOgSvarAktivitetReiseOppstartAvslutningHjemreiseTsoDto)
            return mapAktiviteterReiseOppstartAvslutningHjemreiseTso(type, faktaOgSvar)
        }

        Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR -> {
            require(faktaOgSvar is FaktaOgSvarAktivitetReiseOppstartAvslutningHjemreiseTsrDto)
            return mapAktiviteterReiseOppstartAvslutningHjemreiseTsr(type, faktaOgSvar)
        }

        Stønadstype.FLYTTING_TSO,
        Stønadstype.FLYTTING_TSR,
        -> error("Mapping av aktiviteter for $stønadstype er ikke implementert")
    }
}

private fun mapMålgruppe(
    stønadstype: Stønadstype,
    målgruppe: LagreVilkårperiode,
    fødselFaktaGrunnlag: FødselFaktaGrunnlag?,
): MålgruppeFaktaOgVurdering {
    val type = målgruppe.type
    require(type is MålgruppeType)

    val faktaOgSvar = målgruppe.faktaOgSvar
    require(faktaOgSvar is FaktaOgSvarMålgruppeDto)

    return when (stønadstype) {
        Stønadstype.BARNETILSYN -> {
            mapMålgruppePassAvBarn(type, faktaOgSvar, målgruppe, fødselFaktaGrunnlag)
        }

        Stønadstype.LÆREMIDLER -> {
            mapMålgruppeLæremidler(type, faktaOgSvar, målgruppe, fødselFaktaGrunnlag)
        }

        Stønadstype.BOUTGIFTER -> {
            mapMålgruppeBoutgfiter(type, faktaOgSvar, målgruppe, fødselFaktaGrunnlag)
        }

        Stønadstype.DAGLIG_REISE_TSO -> {
            mapMålgruppeDagligReiseTso(type, faktaOgSvar, målgruppe, fødselFaktaGrunnlag)
        }

        Stønadstype.DAGLIG_REISE_TSR -> {
            mapMålgruppeDagligReiseTsr(type)
        }

        Stønadstype.REISE_TIL_SAMLING_TSO -> {
            mapMålgruppeReiseTilSamlingTso(type, faktaOgSvar, målgruppe, fødselFaktaGrunnlag)
        }

        Stønadstype.REISE_TIL_SAMLING_TSR -> {
            mapMålgruppeReiseTilSamlingTsr(type)
        }

        Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO -> {
            mapMålgruppeReiseOppstartAvslutningHjemreiseTso(type, faktaOgSvar, målgruppe, fødselFaktaGrunnlag)
        }

        Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR -> {
            mapMålgruppeReiseOppstartAvslutningHjemreiseTsr(type)
        }

        Stønadstype.FLYTTING_TSO,
        Stønadstype.FLYTTING_TSR,
        -> error("Mapping av målgruppe for $stønadstype er ikke implementert")
    }
}

private fun mapAktiviteterPassAvBarn(
    aktivitetType: AktivitetType,
    faktaOgSvar: FaktaOgSvarAktivitetPassAvBarnDto,
): AktivitetPassAvBarn =
    when (aktivitetType) {
        AktivitetType.TILTAK -> {
            TiltakPassAvBarn(
                fakta = FaktaAktivitetPassAvBarn(aktivitetsdager = faktaOgSvar.aktivitetsdager!!),
                vurderinger = VurderingTiltakPassAvBarn(lønnet = VurderingLønnet(faktaOgSvar.svarLønnet)),
            )
        }

        AktivitetType.UTDANNING ->
            UtdanningPassAvBarn(
                fakta = FaktaAktivitetPassAvBarn(aktivitetsdager = faktaOgSvar.aktivitetsdager!!),
            )

        AktivitetType.REELL_ARBEIDSSØKER ->
            ReellArbeidsøkerPassAvBarn(
                fakta = FaktaAktivitetPassAvBarn(aktivitetsdager = faktaOgSvar.aktivitetsdager!!),
            )

        AktivitetType.INGEN_AKTIVITET -> {
            feilHvis(faktaOgSvar.aktivitetsdager != null) {
                "Kan ikke registrere aktivitetsdager på ingen aktivitet"
            }
            IngenAktivitetPassAvBarn
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

private fun mapAktiviteterDagligReiseTso(
    aktivitetType: AktivitetType,
    faktaOgSvar: FaktaOgSvarAktivitetDagligReiseTsoDto,
): AktivitetDagligReiseTso =
    when (aktivitetType) {
        AktivitetType.TILTAK -> {
            TiltakDagligReiseTso(
                vurderinger =
                    VurderingTiltakDagligReiseTso(
                        lønnet = VurderingLønnet(faktaOgSvar.svarLønnet),
                        harUtgifter = VurderingHarUtgifter(faktaOgSvar.svarHarUtgifter),
                    ),
                fakta =
                    FaktaAktivitetDagligReiseTso(
                        faktaOgSvar.aktivitetsdager,
                    ),
            )
        }

        AktivitetType.UTDANNING ->
            UtdanningDagligReiseTso(
                vurderinger =
                    VurderingUtdanningDagligReiseTso(
                        harUtgifter = VurderingHarUtgifter(faktaOgSvar.svarHarUtgifter),
                    ),
                fakta = FaktaAktivitetDagligReiseTso(faktaOgSvar.aktivitetsdager),
            )

        AktivitetType.INGEN_AKTIVITET -> IngenAktivitetDagligReiseTso
        AktivitetType.REELL_ARBEIDSSØKER -> feil("Reell arbeidssøker er ikke en gyldig aktivitet for daglige reiser TSO")
    }

private fun mapAktiviteterDagligReiseTsr(
    aktivitetType: AktivitetType,
    faktaOgSvar: FaktaOgSvarAktivitetDagligReiseTsrDto,
): AktivitetDagligReiseTsr =
    when (aktivitetType) {
        AktivitetType.TILTAK -> {
            TiltakDagligReiseTsr(
                vurderinger =
                    VurderingTiltakDagligReiseTsr(
                        harUtgifter = VurderingHarUtgifter(faktaOgSvar.svarHarUtgifter),
                    ),
                fakta = FaktaAktivitetDagligReiseTsr(faktaOgSvar.aktivitetsdager),
            )
        }

        AktivitetType.UTDANNING -> {
            feil("Utdanning er ikke en gyldig aktivitet for daglige reiser TSR")
        }

        AktivitetType.INGEN_AKTIVITET -> IngenAktivitetDagligReiseTsr
        AktivitetType.REELL_ARBEIDSSØKER -> feil("Reell arbeidssøker er ikke en gyldig aktivitet for daglige reiser TSR")
    }

private fun mapAktiviteterReiseTilSamlingTso(
    aktivitetType: AktivitetType,
    faktaOgSvar: FaktaOgSvarAktivitetReiseTilSamlingTsoDto,
): AktivitetReiseTilSamlingTso =
    when (aktivitetType) {
        AktivitetType.TILTAK -> {
            TiltakReiseTilSamlingTso(
                vurderinger =
                    VurderingTiltakReiseTilSamlingTso(
                        lønnet = VurderingLønnet(faktaOgSvar.svarLønnet),
                    ),
            )
        }

        AktivitetType.UTDANNING -> UtdanningReiseTilSamlingTso

        AktivitetType.INGEN_AKTIVITET -> IngenAktivitetReiseTilSamlingTso
        AktivitetType.REELL_ARBEIDSSØKER -> feil("Reell arbeidssøker er ikke en gyldig aktivitet for reise til samling TSO")
    }

private fun lagVurderingAldersvilkår(
    målgruppe: LagreVilkårperiode,
    fødselFaktaGrunnlag: FødselFaktaGrunnlag?,
): VurderingAldersVilkår =
    VurderingAldersVilkår(
        svar = vurderAldersvilkår(målgruppe, fødselFaktaGrunnlag),
        vurderingFaktaEtterlevelse =
            AldersvilkårVurdering
                .VurderingFaktaEtterlevelseAldersvilkår(
                    fødselsdato = fødselFaktaGrunnlag?.fødselsdato,
                ),
    )

private fun mapMålgruppePassAvBarn(
    type: MålgruppeType,
    faktaOgVurderinger: FaktaOgSvarMålgruppeDto,
    målgruppe: LagreVilkårperiode,
    fødselFaktaGrunnlag: FødselFaktaGrunnlag?,
): MålgruppePassAvBarn =
    when (type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppePassAvBarn
        MålgruppeType.SYKEPENGER_100_PROSENT -> SykepengerPassAvBarn
        MålgruppeType.OMSTILLINGSSTØNAD -> {
            OmstillingsstønadPassAvBarn(
                vurderinger =
                    VurderingOmstillingsstønad(
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.OVERGANGSSTØNAD -> {
            OvergangssstønadPassAvBarn
        }

        MålgruppeType.AAP -> {
            AAPPassAvBarn(
                vurderinger =
                    VurderingAAP(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.UFØRETRYGD -> {
            UføretrygdPassAvBarn(
                vurderinger =
                    VurderingUføretrygd(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.NEDSATT_ARBEIDSEVNE -> {
            NedsattArbeidsevnePassAvBarn(
                vurderinger =
                    VurderingNedsattArbeidsevne(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                        mottarSykepengerForFulltidsstilling =
                            VurderingMottarSykepengerForFulltidsstilling(
                                faktaOgVurderinger.svarMottarSykepengerForFulltidsstilling,
                            ),
                    ),
            )
        }

        MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger")
        MålgruppeType.TILTAKSPENGER -> error("Håndterer ikke tiltakspenger")
        MålgruppeType.KVALIFISERINGSSTØNAD -> error("Håndterer ikke kvalifiseringsprogram")
        MålgruppeType.INNSATT_I_FENGSEL -> error("Håndterer ikke innsatt i fengsel")
    }

private fun mapMålgruppeLæremidler(
    type: MålgruppeType,
    faktaOgVurderinger: FaktaOgSvarMålgruppeDto,
    målgruppe: LagreVilkårperiode,
    fødselFaktaGrunnlag: FødselFaktaGrunnlag?,
): MålgruppeLæremidler =
    when (type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeLæremidler
        MålgruppeType.SYKEPENGER_100_PROSENT -> SykepengerLæremidler
        MålgruppeType.OMSTILLINGSSTØNAD -> {
            OmstillingsstønadLæremidler(
                vurderinger =
                    VurderingOmstillingsstønad(
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.OVERGANGSSTØNAD -> {
            OvergangssstønadLæremidler
        }

        MålgruppeType.AAP -> {
            AAPLæremidler(
                vurderinger =
                    VurderingAAPLæremidler(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.UFØRETRYGD -> {
            UføretrygdLæremidler(
                vurderinger =
                    VurderingUføretrygdLæremidler(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.NEDSATT_ARBEIDSEVNE -> {
            NedsattArbeidsevneLæremidler(
                vurderinger =
                    VurderingNedsattArbeidsevneLæremidler(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger")
        MålgruppeType.TILTAKSPENGER -> error("Håndterer ikke tiltakspenger")
        MålgruppeType.KVALIFISERINGSSTØNAD -> error("Håndterer ikke kvalifiseringsprogram")
        MålgruppeType.INNSATT_I_FENGSEL -> error("Håndterer ikke innsatt i fengsel")
    }

private fun mapMålgruppeBoutgfiter(
    type: MålgruppeType,
    faktaOgVurderinger: FaktaOgSvarMålgruppeDto,
    målgruppe: LagreVilkårperiode,
    fødselFaktaGrunnlag: FødselFaktaGrunnlag?,
): MålgruppeBoutgifter =
    when (type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeBoutgifter
        MålgruppeType.OMSTILLINGSSTØNAD -> {
            OmstillingsstønadBoutgifter(
                vurderinger =
                    VurderingOmstillingsstønad(
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
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
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.UFØRETRYGD -> {
            UføretrygdBoutgifter(
                vurderinger =
                    VurderingUføretrygd(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.NEDSATT_ARBEIDSEVNE -> {
            NedsattArbeidsevneBoutgifter(
                vurderinger =
                    VurderingNedsattArbeidsevne(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                        mottarSykepengerForFulltidsstilling =
                            VurderingMottarSykepengerForFulltidsstilling(
                                faktaOgVurderinger.svarMottarSykepengerForFulltidsstilling,
                            ),
                    ),
            )
        }

        MålgruppeType.SYKEPENGER_100_PROSENT -> error("Støtter ikke sykepenger for boutgifter")
        MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger")
        MålgruppeType.TILTAKSPENGER -> error("Håndterer ikke tiltakspenger")
        MålgruppeType.KVALIFISERINGSSTØNAD -> error("Håndterer ikke kvalifiseringsprogram")
        MålgruppeType.INNSATT_I_FENGSEL -> error("Håndterer ikke innsatt i fengsel")
    }

private fun mapMålgruppeDagligReiseTso(
    type: MålgruppeType,
    faktaOgVurderinger: FaktaOgSvarMålgruppeDto,
    målgruppe: LagreVilkårperiode,
    fødselFaktaGrunnlag: FødselFaktaGrunnlag?,
): MålgruppeDagligReiseTso =
    when (type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeDagligReiseTso
        MålgruppeType.OMSTILLINGSSTØNAD -> {
            OmstillingsstønadDagligReiseTso(
                vurderinger =
                    VurderingOmstillingsstønad(
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.OVERGANGSSTØNAD -> {
            OvergangssstønadDagligReiseTso
        }

        MålgruppeType.AAP -> {
            AAPDagligReiseTso(
                vurderinger =
                    VurderingAAP(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.UFØRETRYGD -> {
            UføretrygdDagligReiseTso(
                vurderinger =
                    VurderingUføretrygd(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.NEDSATT_ARBEIDSEVNE -> {
            NedsattArbeidsevneDagligReiseTso(
                vurderinger =
                    VurderingNedsattArbeidsevne(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                        mottarSykepengerForFulltidsstilling =
                            VurderingMottarSykepengerForFulltidsstilling(
                                faktaOgVurderinger.svarMottarSykepengerForFulltidsstilling,
                            ),
                    ),
            )
        }

        MålgruppeType.SYKEPENGER_100_PROSENT -> error("Støtter ikke sykepenger for daglig reise tso")
        MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger")
        MålgruppeType.TILTAKSPENGER -> error("Håndterer ikke tiltakspenger")
        MålgruppeType.KVALIFISERINGSSTØNAD -> error("Håndterer ikke kvalifiseringsprogram")
        MålgruppeType.INNSATT_I_FENGSEL -> error("Håndterer ikke innsatt i fengsel")
    }

private fun mapMålgruppeDagligReiseTsr(type: MålgruppeType): MålgruppeDagligReiseTsr =
    when (type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeDagligReiseTsr
        MålgruppeType.OMSTILLINGSSTØNAD -> error("Håndterer ikke omstillingstønad")
        MålgruppeType.OVERGANGSSTØNAD -> error("Håndterer ikke overgangsstønad")
        MålgruppeType.AAP -> error("Håndterer ikke aap")
        MålgruppeType.UFØRETRYGD -> error("Håndterer ikke uføretrygd")
        MålgruppeType.NEDSATT_ARBEIDSEVNE -> error("Håndterer ikke nedsattArbeidsevne")
        MålgruppeType.SYKEPENGER_100_PROSENT -> error("Støtter ikke sykepenger for boutgifter")
        MålgruppeType.DAGPENGER -> DagpengerDagligReiseTsr()
        MålgruppeType.TILTAKSPENGER -> TiltakspengerDagligReiseTsr()
        MålgruppeType.KVALIFISERINGSSTØNAD -> KvalifiseringsstønadDagligReiseTsr()
        MålgruppeType.INNSATT_I_FENGSEL -> InnsattIFengselDagligReiseTsr()
    }

private fun mapMålgruppeReiseTilSamlingTso(
    type: MålgruppeType,
    faktaOgVurderinger: FaktaOgSvarMålgruppeDto,
    målgruppe: LagreVilkårperiode,
    fødselFaktaGrunnlag: FødselFaktaGrunnlag?,
): MålgruppeReiseTilSamlingTso =
    when (type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeReiseTilSamlingTso
        MålgruppeType.OMSTILLINGSSTØNAD -> {
            OmstillingsstønadReiseTilSamlingTso(
                vurderinger =
                    VurderingOmstillingsstønad(
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.OVERGANGSSTØNAD -> {
            OvergangssstønadReiseTilSamlingTso
        }

        MålgruppeType.AAP -> {
            AAPReiseTilSamlingTso(
                vurderinger =
                    VurderingAAP(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.UFØRETRYGD -> {
            UføretrygdReiseTilSamlingTso(
                vurderinger =
                    VurderingUføretrygd(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.NEDSATT_ARBEIDSEVNE -> {
            NedsattArbeidsevneReiseTilSamlingTso(
                vurderinger =
                    VurderingNedsattArbeidsevne(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                        mottarSykepengerForFulltidsstilling =
                            VurderingMottarSykepengerForFulltidsstilling(
                                faktaOgVurderinger.svarMottarSykepengerForFulltidsstilling,
                            ),
                    ),
            )
        }

        MålgruppeType.SYKEPENGER_100_PROSENT -> error("Støtter ikke sykepenger for reise til samling tso")
        MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger for reise til samling tso")
        MålgruppeType.TILTAKSPENGER -> error("Håndterer ikke tiltakspenger for reise til samling tso")
        MålgruppeType.KVALIFISERINGSSTØNAD -> error("Håndterer ikke kvalifiseringsprogram for reise til samling tso")
        MålgruppeType.INNSATT_I_FENGSEL -> error("Håndterer ikke innsatt i fengsel for reise til samling tso")
    }

private fun mapAktiviteterReiseTilSamlingTsr(aktivitetType: AktivitetType): AktivitetReiseTilSamlingTsr =
    when (aktivitetType) {
        AktivitetType.TILTAK -> TiltakReiseTilSamlingTsr
        AktivitetType.UTDANNING -> UtdanningReiseTilSamlingTsr
        AktivitetType.INGEN_AKTIVITET -> IngenAktivitetReiseTilSamlingTsr
        AktivitetType.REELL_ARBEIDSSØKER -> feil("Reell arbeidssøker er ikke en gyldig aktivitet for reise til samling TSR")
    }

private fun mapMålgruppeReiseTilSamlingTsr(type: MålgruppeType): MålgruppeReiseTilSamlingTsr =
    when (type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeReiseTilSamlingTsr
        MålgruppeType.DAGPENGER -> DagpengerReiseTilSamlingTsr()
        MålgruppeType.TILTAKSPENGER -> TiltakspengerReiseTilSamlingTsr()
        MålgruppeType.KVALIFISERINGSSTØNAD -> KvalifiseringsstønadReiseTilSamlingTsr()
        MålgruppeType.INNSATT_I_FENGSEL -> InnsattIFengselReiseTilSamlingTsr()
        MålgruppeType.OMSTILLINGSSTØNAD -> error("Håndterer ikke omstillingsstønad for reise til samling TSR")
        MålgruppeType.OVERGANGSSTØNAD -> error("Håndterer ikke overgangsstønad for reise til samling TSR")
        MålgruppeType.AAP -> error("Håndterer ikke AAP for reise til samling TSR")
        MålgruppeType.UFØRETRYGD -> error("Håndterer ikke uføretrygd for reise til samling TSR")
        MålgruppeType.NEDSATT_ARBEIDSEVNE -> error("Håndterer ikke nedsatt arbeidsevne for reise til samling TSR")
        MålgruppeType.SYKEPENGER_100_PROSENT -> error("Støtter ikke sykepenger for reise til samling TSR")
    }

private fun mapAktiviteterReiseOppstartAvslutningHjemreiseTso(
    aktivitetType: AktivitetType,
    faktaOgSvar: FaktaOgSvarAktivitetReiseOppstartAvslutningHjemreiseTsoDto,
): AktivitetReiseOppstartAvslutningHjemreiseTso =
    when (aktivitetType) {
        AktivitetType.TILTAK -> {
            TiltakReiseOppstartAvslutningHjemreiseTso(
                vurderinger =
                    VurderingTiltakReiseOppstartAvslutningHjemreiseTso(
                        lønnet = VurderingLønnet(faktaOgSvar.svarLønnet),
                        harUtgifter = VurderingHarUtgifter(faktaOgSvar.svarHarUtgifter),
                        erAktivitetenObligatorisk = VurderingErAktivitetenObligatorisk(faktaOgSvar.svarErAktivitetenObligatorisk),
                    ),
            )
        }

        AktivitetType.UTDANNING ->
            UtdanningReiseOppstartAvslutningHjemreiseTso(
                vurderinger =
                    VurderingUtdanningReiseOppstartAvslutningHjemreiseTso(
                        harUtgifter = VurderingHarUtgifter(faktaOgSvar.svarHarUtgifter),
                        erAktivitetenObligatorisk = VurderingErAktivitetenObligatorisk(faktaOgSvar.svarErAktivitetenObligatorisk),
                    ),
            )

        AktivitetType.INGEN_AKTIVITET -> IngenAktivitetReiseOppstartAvslutningHjemreiseTso
        AktivitetType.REELL_ARBEIDSSØKER ->
            feil(
                "Reell arbeidssøker er ikke en gyldig aktivitet for reise oppstart/avslutning/hjemreise TSO",
            )
    }

private fun mapMålgruppeReiseOppstartAvslutningHjemreiseTso(
    type: MålgruppeType,
    faktaOgVurderinger: FaktaOgSvarMålgruppeDto,
    målgruppe: LagreVilkårperiode,
    fødselFaktaGrunnlag: FødselFaktaGrunnlag?,
): MålgruppeReiseOppstartAvslutningHjemreiseTso =
    when (type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeReiseOppstartAvslutningHjemreiseTso
        MålgruppeType.OMSTILLINGSSTØNAD -> {
            OmstillingsstønadReiseOppstartAvslutningHjemreiseTso(
                vurderinger =
                    VurderingOmstillingsstønad(
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.OVERGANGSSTØNAD -> {
            OvergangssstønadReiseOppstartAvslutningHjemreiseTso
        }

        MålgruppeType.AAP -> {
            AAPReiseOppstartAvslutningHjemreiseTso(
                vurderinger =
                    VurderingAAP(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.UFØRETRYGD -> {
            UføretrygdReiseOppstartAvslutningHjemreiseTso(
                vurderinger =
                    VurderingUføretrygd(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                    ),
            )
        }

        MålgruppeType.NEDSATT_ARBEIDSEVNE -> {
            NedsattArbeidsevneReiseOppstartAvslutningHjemreiseTso(
                vurderinger =
                    VurderingNedsattArbeidsevne(
                        dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(faktaOgVurderinger.svarUtgifterDekketAvAnnetRegelverk),
                        medlemskap = VurderingMedlemskap(faktaOgVurderinger.svarMedlemskap),
                        aldersvilkår = lagVurderingAldersvilkår(målgruppe, fødselFaktaGrunnlag),
                        mottarSykepengerForFulltidsstilling =
                            VurderingMottarSykepengerForFulltidsstilling(
                                faktaOgVurderinger.svarMottarSykepengerForFulltidsstilling,
                            ),
                    ),
            )
        }

        MålgruppeType.SYKEPENGER_100_PROSENT -> error("Støtter ikke sykepenger for reise oppstart/avslutning/hjemreise TSO")
        MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger for reise oppstart/avslutning/hjemreise TSO")
        MålgruppeType.TILTAKSPENGER -> error("Håndterer ikke tiltakspenger for reise oppstart/avslutning/hjemreise TSO")
        MålgruppeType.KVALIFISERINGSSTØNAD -> error("Håndterer ikke kvalifiseringsprogram for reise oppstart/avslutning/hjemreise TSO")
        MålgruppeType.INNSATT_I_FENGSEL -> error("Håndterer ikke innsatt i fengsel for reise oppstart/avslutning/hjemreise TSO")
    }

private fun mapAktiviteterReiseOppstartAvslutningHjemreiseTsr(
    aktivitetType: AktivitetType,
    faktaOgSvar: FaktaOgSvarAktivitetReiseOppstartAvslutningHjemreiseTsrDto,
): AktivitetReiseOppstartAvslutningHjemreiseTsr =
    when (aktivitetType) {
        AktivitetType.TILTAK ->
            TiltakReiseOppstartAvslutningHjemreiseTsr(
                vurderinger =
                    VurderingTiltakReiseOppstartAvslutningHjemreiseTsr(
                        lønnet = VurderingLønnet(faktaOgSvar.svarLønnet),
                        harUtgifter = VurderingHarUtgifter(faktaOgSvar.svarHarUtgifter),
                        erAktivitetenObligatorisk = VurderingErAktivitetenObligatorisk(faktaOgSvar.svarErAktivitetenObligatorisk),
                    ),
            )

        AktivitetType.UTDANNING ->
            feil("Utdanning er ikke en gyldig aktivitet for reise oppstart/avslutning/hjemreise TSR")

        AktivitetType.INGEN_AKTIVITET -> IngenAktivitetReiseOppstartAvslutningHjemreiseTsr
        AktivitetType.REELL_ARBEIDSSØKER ->
            feil(
                "Reell arbeidssøker er ikke en gyldig aktivitet for reise oppstart/avslutning/hjemreise TSR",
            )
    }

private fun mapMålgruppeReiseOppstartAvslutningHjemreiseTsr(type: MålgruppeType): MålgruppeReiseOppstartAvslutningHjemreiseTsr =
    when (type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeReiseOppstartAvslutningHjemreiseTsr
        MålgruppeType.DAGPENGER -> DagpengerReiseOppstartAvslutningHjemreiseTsr()
        MålgruppeType.TILTAKSPENGER -> TiltakspengerReiseOppstartAvslutningHjemreiseTsr()
        MålgruppeType.KVALIFISERINGSSTØNAD -> KvalifiseringsstønadReiseOppstartAvslutningHjemreiseTsr()
        MålgruppeType.INNSATT_I_FENGSEL -> InnsattIFengselReiseOppstartAvslutningHjemreiseTsr()
        MålgruppeType.OMSTILLINGSSTØNAD -> error("Håndterer ikke omstillingsstønad for reise oppstart/avslutning/hjemreise TSR")
        MålgruppeType.OVERGANGSSTØNAD -> error("Håndterer ikke overgangsstønad for reise oppstart/avslutning/hjemreise TSR")
        MålgruppeType.AAP -> error("Håndterer ikke AAP for reise oppstart/avslutning/hjemreise TSR")
        MålgruppeType.UFØRETRYGD -> error("Håndterer ikke uføretrygd for reise oppstart/avslutning/hjemreise TSR")
        MålgruppeType.NEDSATT_ARBEIDSEVNE -> error("Håndterer ikke nedsatt arbeidsevne for reise oppstart/avslutning/hjemreise TSR")
        MålgruppeType.SYKEPENGER_100_PROSENT -> error("Støtter ikke sykepenger for reise oppstart/avslutning/hjemreise TSR")
    }
