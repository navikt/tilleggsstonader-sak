package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * @param utgifter map utgifter per [no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn]
 */
data class InnvilgelseTilsynBarnDto(
    val beregningsresultat: BeregningsresultatTilsynBarnDto?,
) : VedtakTilsynBarnDto(TypeVedtak.INNVILGELSE)

data class InnvilgelseTilsynBarnRequest(
    val beregningsresultat: BeregningsresultatTilsynBarnDto?,
) {
    fun tilDto() = InnvilgelseTilsynBarnDto(beregningsresultat)
}

data class Utgift(
    val fom: YearMonth,
    val tom: YearMonth,
    val utgift: Int,
)

data class BeregningsresultatTilsynBarnDto(
    val perioder: List<BeregningsresultatForMånedDto>,
    val gjelderFraOgMed: LocalDate?,
    val gjelderTilOgMed: LocalDate?,
)

data class BeregningsresultatForMånedDto(
    val dagsats: BigDecimal,
    val månedsbeløp: Int,
    val grunnlag: BeregningsgrunnlagDto,
)

data class BeregningsgrunnlagDto(
    val måned: YearMonth,
    val utgifterTotal: Int,
    val antallBarn: Int,
)

fun BeregningsresultatTilsynBarn.tilDto(): BeregningsresultatTilsynBarnDto {
    val stønadsperioder = this.perioder.flatMap { it.grunnlag.stønadsperioderGrunnlag }
    return BeregningsresultatTilsynBarnDto(
        perioder = this.perioder.map(BeregningsresultatForMåned::tilDto),
        gjelderFraOgMed = stønadsperioder.minOfOrNull { it.stønadsperiode.fom },
        gjelderTilOgMed = stønadsperioder.maxOfOrNull { it.stønadsperiode.tom },
    )
}

private fun BeregningsresultatForMåned.tilDto() = BeregningsresultatForMånedDto(
    dagsats = this.dagsats,
    månedsbeløp = this.månedsbeløp,
    grunnlag = this.grunnlag.tilDto(),
)

private fun Beregningsgrunnlag.tilDto() =
    BeregningsgrunnlagDto(
        måned = this.måned,
        utgifterTotal = this.utgifterTotal,
        antallBarn = this.antallBarn,
    )
