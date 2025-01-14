package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.kontrakter.felles.sisteDagIÅret
import no.nav.tilleggsstonader.kontrakter.felles.splitPerÅr
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.lørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import java.time.LocalDate

object LæremidlerPeriodeUtil {

    fun List<Vedtaksperiode>.grupperVedtaksperioderPerLøpendeMåned(): List<UtbetalingPeriode> = this
        .sorted()
        .delVedtaksperiodePerÅr()
        .fold(listOf<UtbetalingPeriode>()) { acc, vedtaksperiode ->
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
        acc: List<UtbetalingPeriode>,
    ): List<UtbetalingPeriode> {
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
    private fun VedtaksperiodeDeltForÅr.overlappendeDelMed(utbetalingPeriode: UtbetalingPeriode): Vedtaksperiode? {
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
        utbetalingPeriode: UtbetalingPeriode,
    ): VedtaksperiodeDeltForÅr =
        this.copy(fom = maxOf(this.fom, utbetalingPeriode.tom.plusDays(1)))

    /**
     * tom settes til minOf tom og årets tom for å håndtere at den ikke går over 2 år
     */
    private fun VedtaksperiodeDeltForÅr.delTilUtbetalingPerioder(): List<UtbetalingPeriode> {
        return this.splitPerLøpendeMåneder { fom, tom ->
            UtbetalingPeriode(
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

    /**
     * Splitter en periode i løpende måneder. Løpende måned er fra dagens dato og en måned frem i tiden.
     * eks 05.01.2024-29.02.24 blir listOf( P(fom=05.01.2024,tom=04.02.2024), P(fom=05.02.2024,tom=29.02.2024) )
     */
    fun <P : Periode<LocalDate>, VAL : Periode<LocalDate>> P.splitPerLøpendeMåneder(
        medNyPeriode: (fom: LocalDate, tom: LocalDate) -> VAL,
    ): List<VAL> {
        val perioder = mutableListOf<VAL>()
        var gjeldendeFom = fom
        while (gjeldendeFom <= tom) {
            val nyTom = minOf(gjeldendeFom.sisteDagenILøpendeMåned(), tom)
            val nyPeriode = medNyPeriode(gjeldendeFom, nyTom)
            if (nyPeriode.harDatoerIUkedager()) {
                perioder.add(nyPeriode)
            }

            gjeldendeFom = nyTom.plusDays(1)
        }
        return perioder
    }

    fun LocalDate.sisteDagenILøpendeMåned(): LocalDate =
        this.plusMonths(1).minusDays(1)

    private fun <P : Periode<LocalDate>> P.harDatoerIUkedager(): Boolean = this.alleDatoer()
        .any { dato -> !dato.lørdagEllerSøndag() }
}
