package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.sisteDagIÅret
import no.nav.tilleggsstonader.sak.util.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BoutgifterPerUtgiftstype
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.splitPerLøpendeMåneder
import java.time.LocalDate
import kotlin.math.min

object BoutgifterBeregnUtil {
    /**
     * Splitter vedtaksperioder i segmenter ved grensedatoene til faktiske utgifter.
     * Hvert segment beregnes uavhengig med sin egen periodetelling, slik at faktiske utgifter
     * ikke trenger å følge de samme 30-dagers-vinduene som de normale utgiftene.
     *
     * Eksempel: VP 15.09.2025–31.05.2026 med grensedato 01.03.2026 gir
     * - Segment 1: VP(15.09.2025–28.02.2026)
     * - Segment 2: VP(01.03.2026–31.05.2026)
     */
    fun List<VedtaksperiodeBeregning>.splittVedGrensenTilFaktiskeUtgifter(
        grensedatoer: List<LocalDate>,
    ): List<List<VedtaksperiodeBeregning>> {
        if (grensedatoer.isEmpty()) return listOf(this)

        val grensedato = grensedatoer.first()

        val segmentMedNormaleUtgifter =
            mapNotNull { vedtaksperiode ->
                when {
                    vedtaksperiode.fom >= grensedato -> null
                    vedtaksperiode.tom < grensedato -> vedtaksperiode
                    else -> vedtaksperiode.copy(tom = grensedato.minusDays(1))
                }
            }

        val sekgmentMedfaktiskeOgResterendeUtgfifter =
            mapNotNull { vedtaksperiode ->
                when {
                    vedtaksperiode.tom < grensedato -> null
                    vedtaksperiode.fom >= grensedato -> vedtaksperiode
                    else -> vedtaksperiode.copy(fom = grensedato)
                }
            }

        val segmenter = mutableListOf<List<VedtaksperiodeBeregning>>()
        if (segmentMedNormaleUtgifter.isNotEmpty()) {
            segmenter.add(segmentMedNormaleUtgifter)
        }
        segmenter.addAll(sekgmentMedfaktiskeOgResterendeUtgfifter.splittVedGrensenTilFaktiskeUtgifter(grensedatoer.drop(1)))
        return segmenter
    }

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
                    val nyeUtbetalingsperioder = vedtaksperiode.delTilUtbetalingPerioder()
                    acc + nyeUtbetalingsperioder
                } else {
                    val håndterNyUtbetalingsperiode = vedtaksperiode.håndterNyUtbetalingsperiode(acc)
                    acc + håndterNyUtbetalingsperiode
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
