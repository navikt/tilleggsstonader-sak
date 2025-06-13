package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat

object BoutgifterBeregningServiceFeilmeldingUtil {
    fun lagDetFinnesUtgifterSomKrysserUtbetlingsperioderFeilmelding(
        utgifter: List<UtgiftBeregningBoutgifter>,
        utbetalingsperioder: List<UtbetalingPeriode>,
    ): String =
        buildString {
            appendLine("Utgiftsperioder krysser beregningsperioder")
            appendLine()
            appendLine(
                lagPunktlisteMedOverlappendeUtgifterOgBeregningsperioder(
                    utgifter = utgifter,
                    utbetalingsperioder = utbetalingsperioder,
                ),
            )
            appendLine()
            appendLine("Utgiftsperioden(e) må splittes.")
        }

    private fun lagPunktlisteMedOverlappendeUtgifterOgBeregningsperioder(
        utgifter: List<UtgiftBeregningBoutgifter>,
        utbetalingsperioder: List<UtbetalingPeriode>,
    ): String =
        utgifter
            .sorted()
            .filter { utgift -> utgift.overlapperFlereUtbetalingsperioder(utbetalingsperioder) }
            .joinToString(separator = "\n\n") { utgift ->
                "Utgiftsperiode ${utgift.formatertPeriodeNorskFormat()} krysser beregningsperiodene: \n ${
                    utgift.finnOverlappendeUtbetalingsperioder(
                        utbetalingsperioder,
                    )
                        .joinToString("\n ") { utbetalingsperiode -> "- ${utbetalingsperiode.formatertPeriodeNorskFormat()}" }
                }"
            }

    private fun UtgiftBeregningBoutgifter.finnOverlappendeUtbetalingsperioder(
        utbetalingsperioder: List<UtbetalingPeriode>,
    ): List<UtbetalingPeriode> = utbetalingsperioder.filter { it.overlapper(this) }

    private fun UtgiftBeregningBoutgifter.overlapperFlereUtbetalingsperioder(utbetalingsperioder: List<UtbetalingPeriode>): Boolean =
        finnOverlappendeUtbetalingsperioder(utbetalingsperioder).size > 1
}
