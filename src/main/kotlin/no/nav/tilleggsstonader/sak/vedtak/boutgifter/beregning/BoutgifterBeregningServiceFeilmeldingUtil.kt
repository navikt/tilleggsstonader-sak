package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat

object BoutgifterBeregningServiceFeilmeldingUtil {
    fun lagDetFinnesUtgifterSomKrysserUtbetlingsperioderFeilmelding(
        utgifter: List<UtgiftBeregningBoutgifter>,
        utbetalingsperioder: List<UtbetalingPeriode>,
    ): String =
        buildString {
            appendLine("Systemet klarer ikke å beregne utgifter til overnatting som krysser beregningsperioder")
            appendLine("Følgende perioder med overnatting må splittes i to perioder for å kunne beregne")
            appendLine()
            append(
                lagOverlappendeUtgifterOgBeregningsperioderAvsnitt(
                    utgifter = utgifter,
                    utbetalingsperioder = utbetalingsperioder,
                ),
            )
        }

    private fun lagOverlappendeUtgifterOgBeregningsperioderAvsnitt(
        utgifter: List<UtgiftBeregningBoutgifter>,
        utbetalingsperioder: List<UtbetalingPeriode>,
    ): String =
        utgifter
            .filter { it.harFlereOverlappendeUtbetalingsperioder(utbetalingsperioder) }
            .sorted()
            .joinToString(separator = "\n") { it.tilOverlappendeUtgiftOgBeregningsperioderString(utbetalingsperioder) }

    private fun UtgiftBeregningBoutgifter.tilOverlappendeUtgiftOgBeregningsperioderString(
        utbetalingsperioder: List<UtbetalingPeriode>,
    ): String =
        buildString {
            append("${formatertPeriodeNorskFormat()} må splittes til${tilOverlappendePerioderPunktliste(utbetalingsperioder)}")
        }

    private fun UtgiftBeregningBoutgifter.tilOverlappendePerioderPunktliste(utbetalingsperioder: List<UtbetalingPeriode>): String =
        finnOverlappendeUtbetalingsperioder(utbetalingsperioder)
            .joinToString(" og") { " ${it.formatertPeriodeNorskFormat()}" }

    private fun UtgiftBeregningBoutgifter.finnOverlappendeUtbetalingsperioder(
        utbetalingsperioder: List<UtbetalingPeriode>,
    ): List<UtbetalingPeriode> = utbetalingsperioder.filter { it.overlapper(this) }

    private fun UtgiftBeregningBoutgifter.harFlereOverlappendeUtbetalingsperioder(utbetalingsperioder: List<UtbetalingPeriode>): Boolean =
        finnOverlappendeUtbetalingsperioder(utbetalingsperioder).size > 1
}
