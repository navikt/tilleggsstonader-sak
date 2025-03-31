package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.sisteDagIÅret
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterVedtaksperiodeUtil.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterVedtaksperiodeUtil.splitPerLøpendeMåneder
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import kotlin.collections.plus

object BoutgifterBeregnUtil {
    /**
     * Splitter opp vedtaksperioder på løpende måneder.
     *
     * Hvis en vedtaksperiode løper lengre enn første måned vil det bli en ny periode, med ny utbetalingsdato.
     */
    fun List<VedtaksperiodeBeregning>.splittTilLøpendeMåneder(): List<LøpendeMåned> =
        this
            .sorted()
            .fold(listOf<LøpendeMåned>()) { acc, vedtaksperiode ->
                if (acc.isEmpty()) {
                    val nyeUtbetalingsperioder = vedtaksperiode.delTilUtbetalingPerioder()
                    acc + nyeUtbetalingsperioder
                } else {
                    val håndterNyUtbetalingsperiode = vedtaksperiode.håndterNyUtbetalingsperiode(acc)
                    acc + håndterNyUtbetalingsperiode
                }
            }

    /**
     * Legger til periode som overlapper med forrige utbetalingsperiode
     * Returnerer utbetalingsperioder som løper etter forrige utbetalingsperiode
     */
    private fun VedtaksperiodeBeregning.håndterNyUtbetalingsperiode(acc: List<LøpendeMåned>): List<LøpendeMåned> {
        val forrigeUtbetalingsperiode = acc.last()
        forrigeUtbetalingsperiode.leggTilOverlappendeDel(this)

        return lagUtbetalingPerioderEtterForrigeUtbetalingperiode(forrigeUtbetalingsperiode)
    }

    private fun LøpendeMåned.leggTilOverlappendeDel(vedtaksperiode: VedtaksperiodeBeregning) {
        if (vedtaksperiode.fom <= this.tom) {
            val overlappendeVedtaksperiode =
                VedtaksperiodeInnenforLøpendeMåned(
                    fom = vedtaksperiode.fom,
                    tom = minOf(this.tom, vedtaksperiode.tom),
                    målgruppe = vedtaksperiode.målgruppe,
                    aktivitet = vedtaksperiode.aktivitet,
                )
            this.medVedtaksperiode(overlappendeVedtaksperiode)
        }
    }

    private fun VedtaksperiodeBeregning.lagUtbetalingPerioderEtterForrigeUtbetalingperiode(forrigeUtbetalingsperiode: LøpendeMåned) =
        this
            .delEtterUtbetalingsperiode(forrigeUtbetalingsperiode)
            .delTilUtbetalingPerioder()

    /**
     * Splitter vedtaksperiode som løper etter forrige utbetalingsperiode til nye vedtaksperioder
     */
    private fun VedtaksperiodeBeregning.delEtterUtbetalingsperiode(utbetalingPeriode: LøpendeMåned): VedtaksperiodeBeregning? =
        if (this.tom > utbetalingPeriode.tom) {
            this.copy(fom = maxOf(this.fom, utbetalingPeriode.tom.plusDays(1)))
        } else {
            null
        }

    /**
     * tom settes til minOf tom og årets tom for å håndtere at den ikke går over 2 år
     *
     * I tilfelle man har 2 ulike målgrupper innenfor et og samme år, så vil begge resultere i at man betaler ut begge samme dato
     * Men det vil gjøres som 2 ulike andeler då det skal regnskapsføres riktig mot økonomi.
     */
    private fun VedtaksperiodeBeregning?.delTilUtbetalingPerioder(): List<LøpendeMåned> {
        if (this == null) {
            return emptyList()
        }
        return this.splitPerLøpendeMåneder { fom, tom ->
            LøpendeMåned(
                fom = fom,
                tom = minOf(fom.sisteDagenILøpendeMåned(), this.tom.sisteDagIÅret()),
                utbetalingsdato = this.fom.datoEllerNesteMandagHvisLørdagEllerSøndag(),
            ).medVedtaksperiode(
                VedtaksperiodeInnenforLøpendeMåned(
                    fom = fom,
                    tom = tom,
                    målgruppe = this.målgruppe,
                    aktivitet = this.aktivitet,
                ),
            )
        }
    }
}
