package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.statistikk.vedtak.AndelstypeDvh
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.finnMakssats
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
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
    val erMakssats: Boolean? = null,
) {
    data class JsonWrapper(
        val utbetalinger: List<UtbetalingerDvhV2>,
    )

    companion object {
        fun fraDomene(ytelser: List<AndelTilkjentYtelse>, vedtak: Vedtak) = JsonWrapper(
            ytelser.filterNot { it.type == TypeAndel.UGYLDIG }.map { andelTilkjentYtelse ->

                val makssats = andelTilkjentYtelse.finnMakssats(vedtak)

                UtbetalingerDvhV2(
                    fraOgMed = andelTilkjentYtelse.fom,
                    tilOgMed = andelTilkjentYtelse.tom,
                    type = AndelstypeDvh.fraDomene(andelTilkjentYtelse.type),
                    beløp = andelTilkjentYtelse.beløp,
                    makssats = makssats,
                    erMakssats = makssats?.let { makssats <= andelTilkjentYtelse.beløp },
                )
            },
        )

        private fun AndelTilkjentYtelse.finnMakssats(vedtak: Vedtak): Int? {
            return when (vedtak.data) {
                is AvslagLæremidler -> null
                is AvslagTilsynBarn -> null
                is InnvilgelseLæremidler -> null
                is InnvilgelseTilsynBarn -> vedtak.data.finnMakssats(this.fom.toYearMonth())
                is OpphørTilsynBarn -> vedtak.data.finnMakssats(this.fom.toYearMonth())
            }
        }

        private fun InnvilgelseTilsynBarn.finnMakssats(måned: YearMonth): Int? {
            val antallBarn = this.beregningsresultat.perioder.find { it.grunnlag.måned == måned }?.grunnlag?.antallBarn
            return antallBarn?.let { finnMakssats(måned = måned, antallBarn = antallBarn) }
        }

        private fun OpphørTilsynBarn.finnMakssats(måned: YearMonth): Int? {
            val antallBarn = this.beregningsresultat.perioder.find { it.grunnlag.måned == måned }?.grunnlag?.antallBarn
            return antallBarn?.let { finnMakssats(måned = måned, antallBarn = antallBarn) }
        }
    }
}
