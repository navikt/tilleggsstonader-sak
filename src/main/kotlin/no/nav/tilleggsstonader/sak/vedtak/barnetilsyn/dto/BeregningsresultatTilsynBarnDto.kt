package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.YEAR_MONTH_MIN
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeTilsynBarnMapper
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeTilsynBarnMapper.VedtaksperiodeTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class BeregningsresultatTilsynBarnDto(
    val perioder: List<BeregningsresultatForMånedDto>,
    val vedtaksperioder: List<VedtaksperiodeTilsynBarnDto>,
    val gjelderFraOgMed: LocalDate?,
    val gjelderTilOgMed: LocalDate?,
    val tidligsteEndring: LocalDate?,
)

data class BeregningsresultatForMånedDto(
    val dagsats: BigDecimal,
    val månedsbeløp: Int,
    val grunnlag: BeregningsgrunnlagDto,
)

data class VedtaksperiodeTilsynBarnDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
    val antallBarn: Int,
)

data class BeregningsgrunnlagDto(
    val måned: YearMonth,
    val utgifterTotal: Int,
    val antallBarn: Int,
)

/**
 * Beregningsresultat inneholder perioder for nytt vedtak inklusive perioder som er kopiert fra forrige behandling
 * Men det er i de fleste tilfeller kun interessant å vise perioder fra og med tidligsteEndring.
 */
fun BeregningsresultatTilsynBarn.tilDto(tidligsteEndring: LocalDate?): BeregningsresultatTilsynBarnDto {
    val filtrertPerioder =
        this.perioder
            .filterNot { it.grunnlag.måned < (tidligsteEndring?.toYearMonth() ?: YEAR_MONTH_MIN) }

    val vedtaksperioder =
        VedtaksperiodeTilsynBarnMapper
            .mapTilVedtaksperiode(this.perioder)
            .filtrerVedtaksperioderFra(tidligsteEndring)
            .map { it.tilDto() }

    return BeregningsresultatTilsynBarnDto(
        perioder = filtrertPerioder.map { it.tilDto(tidligsteEndring) },
        vedtaksperioder = vedtaksperioder,
        gjelderFraOgMed = vedtaksperioder.minOfOrNull { it.fom },
        gjelderTilOgMed = vedtaksperioder.maxOfOrNull { it.tom },
        tidligsteEndring = tidligsteEndring,
    )
}

private fun VedtaksperiodeTilsynBarn.tilDto() =
    VedtaksperiodeTilsynBarnDto(
        fom = fom,
        tom = tom,
        målgruppe = målgruppe,
        aktivitet = aktivitet,
        antallBarn = antallBarn,
    )

private fun BeregningsresultatForMåned.tilDto(tidligsteEndring: LocalDate?): BeregningsresultatForMånedDto {
    val filtrerteBeløpsperioder = this.beløpsperioder.filtrerBeløpsperioderFra(tidligsteEndring)

    return BeregningsresultatForMånedDto(
        dagsats = this.dagsats,
        månedsbeløp = filtrerteBeløpsperioder.sumOf { it.beløp },
        grunnlag = this.grunnlag.tilDto(),
    )
}

private fun Beregningsgrunnlag.tilDto() =
    BeregningsgrunnlagDto(
        måned = this.måned,
        utgifterTotal = this.utgifterTotal,
        antallBarn = this.antallBarn,
    )

/**
 * Skal kun ha med beløpsperioder som er lik eller etter tidligsteendring
 */
private fun List<Beløpsperiode>.filtrerBeløpsperioderFra(tidligsteendring: LocalDate?) =
    mapNotNull {
        when {
            tidligsteendring == null -> it
            it.dato < tidligsteendring -> null
            else -> it
        }
    }

/**
 * Skal kun ha med vedtaksperioden som er etter [tidligsteEndring]
 * Dersom vedtaksperioden overlapper med [tidligsteEndring] så skal den avkortes fra og med tidligsteEndring-dato
 */
private fun List<VedtaksperiodeTilsynBarn>.filtrerVedtaksperioderFra(tidligsteEndring: LocalDate?): List<VedtaksperiodeTilsynBarn> =
    mapNotNull {
        when {
            tidligsteEndring == null -> it
            it.tom < tidligsteEndring -> null
            else -> it.copy(fom = maxOf(it.fom, tidligsteEndring))
        }
    }
