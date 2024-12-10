package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import java.time.LocalDate
import java.time.YearMonth
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

data class BeregningsresultatLæremidler(
    val perioder: List<BeregningsresultatForMåned>,
) {
    fun tilDto(): BeregningsresultatLæremidlerDto {
        val perioderDto = perioder.map { it.tilDto() }
        return BeregningsresultatLæremidlerDto(
            perioder = perioderDto.mergeSammenhengende(
                skalMerges = { v1, v2 -> kanSlåsSammen(v1, v2) },
                merge = { v1, v2 -> slåSammen(v1, v2) },
            ),
        )
    }

    private fun slåSammen(
        gjeldenePeriode: BeregningsresultatForPeriodeDto,
        nestePeriode: BeregningsresultatForPeriodeDto,
    ): BeregningsresultatForPeriodeDto {
        return gjeldenePeriode.copy(
            tom = nestePeriode.tom,
            stønadsbeløp = gjeldenePeriode.stønadsbeløp + nestePeriode.stønadsbeløp,
            antallMåneder = gjeldenePeriode.antallMåneder + 1,
        )
    }

    private fun kanSlåsSammen(
        gjeldenePeriode: BeregningsresultatForPeriodeDto,
        nestePeriode: BeregningsresultatForPeriodeDto,
    ): Boolean {
        return gjeldenePeriode.studienivå == nestePeriode.studienivå &&
                gjeldenePeriode.studieprosent == nestePeriode.studieprosent &&
                gjeldenePeriode.sats == nestePeriode.sats &&
                gjeldenePeriode.utbetalingsmåned == nestePeriode.utbetalingsmåned &&
                gjeldenePeriode.påfølgesAv(nestePeriode)
    }
}

data class BeregningsresultatForMåned(
    val beløp: Int,
    val grunnlag: Beregningsgrunnlag,
) {
    fun tilDto(): BeregningsresultatForPeriodeDto {
        return BeregningsresultatForPeriodeDto(
            fom = grunnlag.fom,
            tom = grunnlag.tom,
            antallMåneder = 1,
            studienivå = grunnlag.studienivå,
            studieprosent = grunnlag.studieprosent,
            sats = grunnlag.sats,
            stønadsbeløp = beløp,
            utbetalingsmåned = grunnlag.utbetalingsMåned,
        )
    }
}

data class Beregningsgrunnlag(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utbetalingsMåned: YearMonth,
    val studienivå: Studienivå,
    val studieprosent: Int,
    val sats: Int,
    val målgruppe: MålgruppeType,
) : Periode<LocalDate>

data class BeregningsresultatLæremidlerDto(
    val perioder: List<BeregningsresultatForPeriodeDto>,
)

data class BeregningsresultatForPeriodeDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val antallMåneder: Int,
    val studienivå: Studienivå,
    val studieprosent: Int,
    val sats: Int,
    val stønadsbeløp: Int,
    val utbetalingsmåned: YearMonth,
) : Periode<LocalDate>
