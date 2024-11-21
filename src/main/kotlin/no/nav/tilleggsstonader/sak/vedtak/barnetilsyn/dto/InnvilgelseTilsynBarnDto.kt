package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.util.YEAR_MONTH_MIN
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeMapper
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class InnvilgelseTilsynBarnDto(
    val beregningsresultat: BeregningsresultatTilsynBarnDto?,
) : VedtakTilsynBarnDto(TypeVedtak.INNVILGELSE)

data class InnvilgelseTilsynBarnRequest(
    val beregningsresultat: BeregningsresultatTilsynBarnDto?,
) {
    fun tilDto() = InnvilgelseTilsynBarnDto(beregningsresultat)
}

data class BeregningsresultatTilsynBarnDto(
    val perioder: List<BeregningsresultatForMånedDto>,
    val vedtaksperioder: List<VedtaksperiodeDto>,
    val gjelderFraOgMed: LocalDate?,
    val gjelderTilOgMed: LocalDate?,
)

data class BeregningsresultatForMånedDto(
    val dagsats: BigDecimal,
    val månedsbeløp: Int,
    val grunnlag: BeregningsgrunnlagDto,
)

data class VedtaksperiodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppe: MålgruppeType,
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
 * Men det er i de fleste tilfeller kun interessant å vise perioder fra og med revurderFra
 */
fun BeregningsresultatTilsynBarn.tilDto(
    revurderFra: LocalDate?,
): BeregningsresultatTilsynBarnDto {
    val filtrertPerioder = this.perioder
        .filterNot { it.grunnlag.måned < (revurderFra?.toYearMonth() ?: YEAR_MONTH_MIN) }

    val vedtaksperioder = VedtaksperiodeMapper.mapTilVedtaksperiode(this.perioder)
        .filtrerStønadsperioderFra(revurderFra)
        .map { it.tilDto() }

    return BeregningsresultatTilsynBarnDto(
        perioder = filtrertPerioder.map { it.tilDto(revurderFra) },
        vedtaksperioder = vedtaksperioder,
        gjelderFraOgMed = vedtaksperioder.minOfOrNull { it.fom },
        gjelderTilOgMed = vedtaksperioder.maxOfOrNull { it.tom },
    )
}

private fun Vedtaksperiode.tilDto() = VedtaksperiodeDto(
    fom = fom,
    tom = tom,
    målgruppe = målgruppe,
    aktivitet = aktivitet,
    antallBarn = antallBarn,
)

private fun BeregningsresultatForMåned.tilDto(revurderFra: LocalDate?): BeregningsresultatForMånedDto {
    val filtrerteBeløpsperioder = this.beløpsperioder.filtrerBeløpsperioderFra(revurderFra)

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
 * Skal kun ha med beløpsperioder som er lik eller etter revurderFra
 */
private fun List<Beløpsperiode>.filtrerBeløpsperioderFra(revurderFra: LocalDate?) = mapNotNull {
    when {
        revurderFra == null -> it
        it.dato < revurderFra -> null
        else -> it
    }
}

/**
 * Skal kun ha med stønadsperioder som er etter [revurderFra]
 * Dersom stønadsperioden overlapper med [revurderFra] så skal den avkortes fra og med revurderFra-dato
 */
private fun List<Vedtaksperiode>.filtrerStønadsperioderFra(
    revurderFra: LocalDate?,
): List<Vedtaksperiode> = mapNotNull {
    when {
        revurderFra == null -> it
        it.tom < revurderFra -> null
        else -> it.copy(fom = maxOf(it.fom, revurderFra))
    }
}
