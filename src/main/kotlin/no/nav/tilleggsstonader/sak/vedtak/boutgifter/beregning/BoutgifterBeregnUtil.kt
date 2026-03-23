package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.sisteDagIÅret
import no.nav.tilleggsstonader.sak.felles.Tidslinje
import no.nav.tilleggsstonader.sak.util.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BoutgifterPerUtgiftstype
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.splitPerLøpendeMåneder
import kotlin.math.min

object BoutgifterBeregnUtil {
    fun List<VedtaksperiodeBeregning>.splittVedGrensenTilFaktiskeUtgifter(
        utgifter: BoutgifterPerUtgiftstype,
    ): List<List<VedtaksperiodeBeregning>> {
        val cutDates = utgifter.finnStartdatoForFaktiskeUtgifter()
        if (cutDates.isEmpty()) return listOf(this)
        return Tidslinje(this).grupperVedDatoer(cutDates)
    }

    private fun BoutgifterPerUtgiftstype.finnStartdatoForFaktiskeUtgifter() =
        values
            .flatten()
            .filter { it.skalFåDekketFaktiskeUtgifter }
            .map { it.fom }
            .distinct()
            .sorted()

    /**
     * Splitter opp vedtaksperioder på løpende måneder.
     *
     * Hvis en vedtaksperiode løper lengre enn første måned vil det bli en ny periode, med ny utbetalingsdato.
     */
    fun List<VedtaksperiodeBeregning>.splittTilLøpendeMåneder(): List<LøpendeMåned> =
        this
            .sorted()
            .fold(listOf()) { acc, vedtaksperiode ->
                if (acc.isEmpty()) {
                    acc + vedtaksperiode.delTilUtbetalingPerioder()
                } else {
                    val (oppdatertForrige, nyePerioder) = vedtaksperiode.håndterNyUtbetalingsperiode(acc.last())
                    acc.dropLast(1) + oppdatertForrige + nyePerioder
                }
            }

    fun Beregningsgrunnlag.beregnStønadsbeløp(): Int {
        val sumUtgifter = summerUtgifter()
        if (this.skalFåDekketFaktiskeUtgifter()) {
            return sumUtgifter
        }
        return min(sumUtgifter, makssats)
    }

    fun Beregningsgrunnlag.summerUtgifter() =
        utgifter.values
            .flatten()
            .sumOf { it.utgift }

    fun lagBeregningsgrunnlag(
        periode: UtbetalingPeriode,
        utgifter: BoutgifterPerUtgiftstype,
        makssats: MakssatsBoutgifter,
    ): Beregningsgrunnlag {
        val utgifterIPerioden =
            utgifter.mapValues { (_, utgifter) ->
                utgifter.filter {
                    periode.overlapper(it)
                }
            }

        return Beregningsgrunnlag(
            fom = periode.fom,
            tom = periode.tom,
            utgifter = utgifterIPerioden,
            makssats = makssats.beløp,
            makssatsBekreftet = makssats.bekreftet,
            målgruppe = periode.målgruppe,
            aktivitet = periode.aktivitet,
        )
    }

    private fun VedtaksperiodeBeregning.håndterNyUtbetalingsperiode(
        forrigeUtbetalingsperiode: LøpendeMåned,
    ): Pair<LøpendeMåned, List<LøpendeMåned>> {
        val oppdatert = forrigeUtbetalingsperiode.leggTilOverlappendeDel(this)
        val nyePerioder = this.delEtterUtbetalingsperiode(oppdatert).delTilUtbetalingPerioder()
        return oppdatert to nyePerioder
    }

    private fun LøpendeMåned.leggTilOverlappendeDel(vedtaksperiode: VedtaksperiodeBeregning): LøpendeMåned {
        if (vedtaksperiode.fom > this.tom) return this
        val overlappendeVedtaksperiode =
            VedtaksperiodeInnenforLøpendeMåned(
                fom = vedtaksperiode.fom,
                tom = minOf(this.tom, vedtaksperiode.tom),
                målgruppe = vedtaksperiode.målgruppe,
                aktivitet = vedtaksperiode.aktivitet,
            )
        return this.medVedtaksperiode(overlappendeVedtaksperiode)
    }

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
