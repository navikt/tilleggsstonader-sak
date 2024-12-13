package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import java.time.LocalDate
import java.time.YearMonth

data class BeregningsresultatLæremidlerDto(
    val perioder: List<BeregningsresultatForPeriodeDto>,
)

data class BeregningsresultatForPeriodeDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val antallMåneder: Int,
    val studienivå: Studienivå,
    val studieprosent: Int,
    val beløp: Int,
    val stønadsbeløp: Int,
    val utbetalingsmåned: YearMonth,
) : Periode<LocalDate> {
    fun slåSammen(
        nestePeriode: BeregningsresultatForPeriodeDto,
    ): BeregningsresultatForPeriodeDto {
        return this.copy(
            tom = nestePeriode.tom,
            stønadsbeløp = this.stønadsbeløp + nestePeriode.stønadsbeløp,
            antallMåneder = this.antallMåneder + 1,
        )
    }

    fun kanSlåsSammen(
        nestePeriode: BeregningsresultatForPeriodeDto,
    ): Boolean {
        return this.studienivå == nestePeriode.studienivå &&
            this.studieprosent == nestePeriode.studieprosent &&
            this.beløp == nestePeriode.beløp &&
            this.utbetalingsmåned == nestePeriode.utbetalingsmåned &&
            this.påfølgesAv(nestePeriode)
    }
}

fun BeregningsresultatLæremidler.tilDto(): BeregningsresultatLæremidlerDto {
    val perioderDto = perioder.map { it.tilDto() }.sorted()
    return BeregningsresultatLæremidlerDto(
        perioder = perioderDto.mergeSammenhengende(
            skalMerges = { v1, v2 -> v1.kanSlåsSammen(v2) },
            merge = { v1, v2 -> v1.slåSammen(v2) },
        ),
    )
}

fun BeregningsresultatForMåned.tilDto(): BeregningsresultatForPeriodeDto {
    return BeregningsresultatForPeriodeDto(
        fom = grunnlag.fom,
        tom = grunnlag.tom,
        antallMåneder = 1,
        studienivå = grunnlag.studienivå,
        studieprosent = grunnlag.studieprosent,
        beløp = beløp,
        stønadsbeløp = beløp,
        utbetalingsmåned = grunnlag.utbetalingsMåned,
    )
}
