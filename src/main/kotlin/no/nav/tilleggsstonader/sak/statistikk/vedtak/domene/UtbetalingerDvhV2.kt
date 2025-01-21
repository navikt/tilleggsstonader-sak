package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.statistikk.vedtak.AndelstypeDvh
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.finnMakssats
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.takeIfType
import java.time.LocalDate

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
        fun fraDomene(ytelser: List<AndelTilkjentYtelse>, vedtak: Vedtak?) = JsonWrapper(
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

        private fun AndelTilkjentYtelse.finnMakssats(vedtak: Vedtak?): Int? {
            val vedtakBeregningsresultatForMåned =
                vedtak?.takeIfType<InnvilgelseTilsynBarn>()?.data?.beregningsresultat?.perioder
            val antallBarn =
                vedtakBeregningsresultatForMåned?.find { it.grunnlag.måned == this.fom.toYearMonth() }?.grunnlag?.antallBarn
            return antallBarn?.let {
                finnMakssats(
                    this.fom.toYearMonth(),
                    antallBarn,
                )
            }
        }
    }
}
