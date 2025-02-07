package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.statistikk.vedtak.AndelstypeDvh
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV1.DEKNINGSGRAD_TILSYN_BARN
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import java.time.LocalDate
import java.time.YearMonth

data class UtbetalingerDvhV2(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val type: AndelstypeDvh,
    val beløp: Int,
    val makssats: Int? = null,
    val beløpErBegrensetAvMakssats: Boolean? = null,
) {
    data class JsonWrapper(
        val utbetalinger: List<UtbetalingerDvhV2>,
    )

    companion object {
        fun fraDomene(
            ytelser: List<AndelTilkjentYtelse>,
            vedtak: Vedtak,
        ) = JsonWrapper(
            ytelser.filterNot { it.type == TypeAndel.UGYLDIG }.map { andelTilkjentYtelse ->

                val beregningsgrunnlag = andelTilkjentYtelse.finnGrunnlag(vedtak)

                UtbetalingerDvhV2(
                    fraOgMed = andelTilkjentYtelse.fom,
                    tilOgMed = andelTilkjentYtelse.tom,
                    type = AndelstypeDvh.fraDomene(andelTilkjentYtelse.type),
                    beløp = andelTilkjentYtelse.beløp,
                    makssats = beregningsgrunnlag?.makssats,
                    beløpErBegrensetAvMakssats = erBeløpBegrensetAvMakssats(beregningsgrunnlag),
                )
            },
        )

        private fun AndelTilkjentYtelse.finnGrunnlag(vedtak: Vedtak): Beregningsgrunnlag? =
            when (vedtak.data) {
                is AvslagLæremidler -> null
                is AvslagTilsynBarn -> null
                is InnvilgelseLæremidler -> null
                is InnvilgelseTilsynBarn -> vedtak.data.finnGrunnlag(this.fom.toYearMonth())
                is OpphørTilsynBarn -> vedtak.data.finnGrunnlag(this.fom.toYearMonth())
                is OpphørLæremidler -> null
            }

        private fun InnvilgelseTilsynBarn.finnGrunnlag(måned: YearMonth): Beregningsgrunnlag =
            this.beregningsresultat.perioder
                .find { it.grunnlag.måned == måned }
                ?.grunnlag
                ?: error("Skal ha beregningsgrunnlag hvis andeler eksisterer")

        private fun OpphørTilsynBarn.finnGrunnlag(måned: YearMonth): Beregningsgrunnlag =
            this.beregningsresultat.perioder
                .find { it.grunnlag.måned == måned }
                ?.grunnlag
                ?: error("Skal ha beregningsgrunnlag hvis andeler eksisterer")

        private fun erBeløpBegrensetAvMakssats(beregningsgrunnlag: Beregningsgrunnlag?): Boolean? =
            beregningsgrunnlag?.let {
                val utgifterSomDekkes =
                    (beregningsgrunnlag.utgifterTotal.toBigDecimal()).multiply(
                        DEKNINGSGRAD_TILSYN_BARN,
                    )
                utgifterSomDekkes > beregningsgrunnlag.makssats.toBigDecimal()
            }
    }
}
