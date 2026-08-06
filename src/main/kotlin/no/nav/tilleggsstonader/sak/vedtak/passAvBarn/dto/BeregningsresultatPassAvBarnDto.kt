package no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.YEAR_MONTH_MIN
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.dto.BeregningsplanDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.BeregningsresultatPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.VedtaksperiodePassAvBarnMapper
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.VedtaksperiodePassAvBarnMapper.VedtaksperiodePassAvBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class BeregningsresultatPassAvBarnDto(
    val perioder: List<BeregningsresultatForMånedDto>,
    val vedtaksperioder: List<VedtaksperiodePassAvBarnDto>,
    val gjelderFraOgMed: LocalDate?,
    val gjelderTilOgMed: LocalDate?,
    val tidligsteEndring: LocalDate?,
    val beregningsplan: BeregningsplanDto,
)

data class BeregningsresultatForMånedDto(
    val dagsats: BigDecimal,
    val månedsbeløp: Int,
    val grunnlag: BeregningsgrunnlagDto,
)

data class VedtaksperiodePassAvBarnDto(
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
fun BeregningsresultatPassAvBarn.tilDto(beregningsplan: Beregningsplan): BeregningsresultatPassAvBarnDto {
    val filtrertPerioder = this.perioder.filterNot { it.grunnlag.måned < (beregningsplan.fraDato?.toYearMonth() ?: YEAR_MONTH_MIN) }

    val vedtaksperioder =
        VedtaksperiodePassAvBarnMapper
            .mapTilVedtaksperiode(this.perioder)
            .filtrerVedtaksperioderFra(beregningsplan.fraDato)
            .map { it.tilDto() }

    return BeregningsresultatPassAvBarnDto(
        perioder = filtrertPerioder.map { it.tilDto() },
        vedtaksperioder = vedtaksperioder,
        gjelderFraOgMed = vedtaksperioder.minOfOrNull { it.fom },
        gjelderTilOgMed = vedtaksperioder.maxOfOrNull { it.tom },
        beregningsplan = beregningsplan.tilDto(),
        tidligsteEndring = beregningsplan.legacyTidligsteEndring(),
    )
}

private fun VedtaksperiodePassAvBarn.tilDto() =
    VedtaksperiodePassAvBarnDto(
        fom = fom,
        tom = tom,
        målgruppe = målgruppe,
        aktivitet = aktivitet,
        antallBarn = antallBarn,
    )

private fun BeregningsresultatForMåned.tilDto(): BeregningsresultatForMånedDto =
    BeregningsresultatForMånedDto(
        dagsats = this.dagsats,
        månedsbeløp = beløpsperioder.sumOf { it.beløp },
        grunnlag = this.grunnlag.tilDto(),
    )

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
private fun List<VedtaksperiodePassAvBarn>.filtrerVedtaksperioderFra(tidligsteEndring: LocalDate?): List<VedtaksperiodePassAvBarn> =
    mapNotNull {
        when {
            tidligsteEndring == null -> it
            it.tom < tidligsteEndring -> null
            else -> it.copy(fom = maxOf(it.fom, tidligsteEndring))
        }
    }
