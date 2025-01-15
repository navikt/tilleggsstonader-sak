package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.sisteDagIÅret
import no.nav.tilleggsstonader.kontrakter.felles.splitPerÅr
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerSplitPerLøpendeMånedUtil.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerSplitPerLøpendeMånedUtil.splitPerLøpendeMåneder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import java.time.LocalDate

object LæremidlerBeregnUtil {

    /**
     * Splitter vedtaksperiode per år. Sånn at man får en periode for høstterminen og en for vårterminen
     * Dette for å kunne lage en periode for våren som ikke utbetales direkte, men når satsen for det nye året er satt.
     * Eks 2024-08-15 - 2025-06-20 blir 2024-08-15 - 2024-12-31 og 2025-01-01 - 2025-06-20
     */
    fun Periode<LocalDate>.delTilUtbetalingsPerioder(): List<GrunnlagForUtbetalingPeriode> =
        splitPerÅr { fom, tom -> Vedtaksperiode(fom, tom) }
            .flatMap { periode ->
                periode.splitPerLøpendeMåneder { fom, tom ->
                    GrunnlagForUtbetalingPeriode(
                        fom = fom,
                        tom = tom,
                        utbetalingsdato = periode.fom.datoEllerNesteMandagHvisLørdagEllerSøndag(),
                    )
                }
            }

    fun List<Vedtaksperiode>.grupperVedtaksperioderPerLøpendeMåned(): List<GrunnlagForUtbetalingPeriode> = this
        .sorted()
        .delVedtaksperiodePerÅr()
        .fold(listOf<GrunnlagForUtbetalingPeriode>()) { acc, vedtaksperiode ->
            if (acc.isEmpty()) {
                val nyeUtbetalingsperioder = vedtaksperiode.delTilUtbetalingPerioder()
                acc + nyeUtbetalingsperioder
            } else {
                val håndterNyUtbetalingsperiode = vedtaksperiode.håndterNyUtbetalingsperiode(acc)
                acc + håndterNyUtbetalingsperiode
            }
        }
        .toList()

    /**
     * Legger til periode som overlapper med forrige utbetalingsperiode
     * Returnerer utbetalingsperioder som løper etter forrige utbetalingsperiode
     */
    private fun VedtaksperiodeDeltForÅr.håndterNyUtbetalingsperiode(
        acc: List<GrunnlagForUtbetalingPeriode>,
    ): List<GrunnlagForUtbetalingPeriode> {
        val forrigeUtbetalingsperide = acc.last()
        this.overlappendeDelMed(forrigeUtbetalingsperide)?.let {
            forrigeUtbetalingsperide.medVedtaksperiode(it)
        }
        return this
            .delEtterUtbetalingsperiode(forrigeUtbetalingsperide)
            .delTilUtbetalingPerioder()
    }

    /**
     * Splitter en vedtaksperiode i forrige utbetalingsperiode hvis de overlapper
     */
    private fun VedtaksperiodeDeltForÅr.overlappendeDelMed(utbetalingPeriode: GrunnlagForUtbetalingPeriode): Vedtaksperiode? {
        return if (this.fom <= utbetalingPeriode.tom) {
            Vedtaksperiode(
                fom = utbetalingPeriode.fom,
                tom = minOf(utbetalingPeriode.tom, this.tom),
            )
        } else {
            null
        }
    }

    /**
     * Splitter vedtaksperiode som løper etter forrige utbetalingsperiode til nye vedtaksperioder
     */
    private fun VedtaksperiodeDeltForÅr.delEtterUtbetalingsperiode(
        utbetalingPeriode: GrunnlagForUtbetalingPeriode,
    ): VedtaksperiodeDeltForÅr =
        this.copy(fom = maxOf(this.fom, utbetalingPeriode.tom.plusDays(1)))

    /**
     * tom settes til minOf tom og årets tom for å håndtere at den ikke går over 2 år
     */
    private fun VedtaksperiodeDeltForÅr.delTilUtbetalingPerioder(): List<GrunnlagForUtbetalingPeriode> {
        return this.splitPerLøpendeMåneder { fom, tom ->
            GrunnlagForUtbetalingPeriode(
                fom = fom,
                tom = minOf(fom.sisteDagenILøpendeMåned(), this.tom.sisteDagIÅret()),
                utbetalingsdato = this.fom.datoEllerNesteMandagHvisLørdagEllerSøndag(),
            ).medVedtaksperiode(Vedtaksperiode(fom = fom, tom = tom))
        }
    }

    /**
     * Deler vedtaksperiode i år, for å eks innvilge høst og vår i 2 ulike perioder
     * Og der vårterminen får en ny sats
     */
    data class VedtaksperiodeDeltForÅr(
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : Periode<LocalDate> {
        init {
            require(fom.year == tom.year) {
                "Kan ikke være 2 ulike år (${fom.year}, ${tom.year}})"
            }
        }
    }

    fun List<Vedtaksperiode>.delVedtaksperiodePerÅr(): List<VedtaksperiodeDeltForÅr> = this
        .flatMap {
            it.splitPerÅr { fom, tom ->
                VedtaksperiodeDeltForÅr(fom, tom)
            }
        }
}
