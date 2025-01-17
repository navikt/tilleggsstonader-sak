package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.sisteDagIÅret
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerVedtaksperiodeUtil.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerVedtaksperiodeUtil.splitPerLøpendeMåneder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerVedtaksperiodeUtil.splitVedtaksperiodePerÅr
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode

object LæremidlerBeregnUtil {

    /**
     * Grupperer vedtaksperioder innenfor en løpende måned
     * Hvis en vedtaksperiode løper lengre enn første måned vil det bli en ny periode, med nytt utbetalingsdatum
     */
    fun List<Vedtaksperiode>.grupperVedtaksperioderPerLøpendeMåned(): List<LøpendeMåned> = this
        .sorted()
        .splitVedtaksperiodePerÅr()
        .fold(listOf<LøpendeMåned>()) { acc, vedtaksperiode ->
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
    private fun VedtaksperiodeInnenforÅr.håndterNyUtbetalingsperiode(
        acc: List<LøpendeMåned>,
    ): List<LøpendeMåned> {
        val forrigeUtbetalingsperiode = acc.last()
        forrigeUtbetalingsperiode.leggTilOverlappendeDel(this)

        return lagUtbetalingPerioderEtterForrigeUtbetalingperiode(forrigeUtbetalingsperiode)
    }

    private fun LøpendeMåned.leggTilOverlappendeDel(
        vedtaksperiode: VedtaksperiodeInnenforÅr,
    ) {
        if (vedtaksperiode.fom <= this.tom) {
            val overlappendeVedtaksperiode = Vedtaksperiode(
                fom = vedtaksperiode.fom,
                tom = minOf(this.tom, vedtaksperiode.tom),
            )
            this.medVedtaksperiode(overlappendeVedtaksperiode)
        }
    }

    private fun VedtaksperiodeInnenforÅr.lagUtbetalingPerioderEtterForrigeUtbetalingperiode(
        forrigeUtbetalingsperiode: LøpendeMåned,
    ) = this
        .delEtterUtbetalingsperiode(forrigeUtbetalingsperiode)
        .delTilUtbetalingPerioder()

    /**
     * Splitter vedtaksperiode som løper etter forrige utbetalingsperiode til nye vedtaksperioder
     */
    private fun VedtaksperiodeInnenforÅr.delEtterUtbetalingsperiode(
        utbetalingPeriode: LøpendeMåned,
    ): VedtaksperiodeInnenforÅr? {
        return if (this.tom > utbetalingPeriode.tom) {
            this.copy(fom = maxOf(this.fom, utbetalingPeriode.tom.plusDays(1)))
        } else {
            null
        }
    }

    /**
     * tom settes til minOf tom og årets tom for å håndtere at den ikke går over 2 år
     */
    private fun VedtaksperiodeInnenforÅr?.delTilUtbetalingPerioder(): List<LøpendeMåned> {
        if (this == null) {
            return emptyList()
        }
        return this.splitPerLøpendeMåneder { fom, tom ->
            LøpendeMåned(
                fom = fom,
                tom = minOf(fom.sisteDagenILøpendeMåned(), this.tom.sisteDagIÅret()),
                utbetalingsdato = this.fom.datoEllerNesteMandagHvisLørdagEllerSøndag(),
            ).medVedtaksperiode(Vedtaksperiode(fom = fom, tom = tom))
        }
    }
}
