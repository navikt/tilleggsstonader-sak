package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.UtgiftOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.PeriodeMedDager
import no.nav.tilleggsstonader.sak.vedtak.domain.Uke
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.splitPerUke
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.max
import kotlin.math.min

data class ReiseOgVedtaksperioderSnitt(
    val justerteVedtaksperioder: List<Vedtaksperiode>,
    val justertReiseperiode: UtgiftOffentligTransport,
)

fun finnSnittMellomReiseOgVedtaksperioder(
    reise: UtgiftOffentligTransport,
    vedtaksperioder: List<Vedtaksperiode>,
): ReiseOgVedtaksperioderSnitt =
    ReiseOgVedtaksperioderSnitt(
        justerteVedtaksperioder = vedtaksperioder.mapNotNull { it.beregnSnitt(reise) },
        justertReiseperiode =
            reise.copy(
                fom = maxOf(vedtaksperioder.first().fom, reise.fom),
                tom = minOf(vedtaksperioder.last().tom, reise.tom),
            ),
    )

fun finnReisedagerIPeriode(
    vedtaksperiode: Vedtaksperiode,
    antallReisedagerPerUke: Int,
): Int =
    vedtaksperiode
        .splitPerUke { fom, tom ->
            min(antallReisedagerPerUke, antallHverdagerIPeriodeInklusiv(fom, tom))
        }.values
        .sumOf { it.antallDager }

fun finnBilligsteAlternativForTrettidagersPeriode(grunnlag: Beregningsgrunnlag): Int =
    listOfNotNull(
        finnBilligsteKombinasjonAvEnkeltBillettOgSyvdagersBillett(grunnlag),
        grunnlag.pris30dagersbillett,
    ).min()

/**
 * Minimum Cost For Tickets.
 * Doc: https://docs.vultr.com/problem-set/minimum-cost-for-tickets
 */
private fun finnBilligsteKombinasjonAvEnkeltBillettOgSyvdagersBillett(grunnlag: Beregningsgrunnlag): Int? {
    if (grunnlag.prisEnkeltbillett == null && grunnlag.prisSyvdagersbillett == null) return null
    val reisedagerPerUke = finnReisedagerPerUke(grunnlag)
    val reisedagerListe = lagReisedagerListe(reisedagerPerUke)

    // Hvis ingen reisedager er billigste pris 0kr
    if (reisedagerListe.isEmpty()) return 0

    val sisteReiseDag = reisedagerListe.last()
    val reisekostnader = MutableList(sisteReiseDag + 1) { BillettyperMedPris.tom() }

    var reisedagIndeks = 0
    for (gjeldeneDag in 1..sisteReiseDag) {
        if (gjeldeneDag.skalIkkeReise(reisedagerListe, reisedagIndeks)) {
            reisekostnader[gjeldeneDag] = reisekostnader[gjeldeneDag - 1]
        } else {
            reisedagIndeks++
            reisekostnader[gjeldeneDag] =
                listOfNotNull(
                    finnReisekostnadForNyEnkeltbillett(gjeldeneDag, reisekostnader, grunnlag),
                    finnReisekostnadForNySyvdagersbillett(gjeldeneDag, reisekostnader, grunnlag),
                ).min()
        }
    }
    return reisekostnader[sisteReiseDag].also { println(it) }.pris
}

data class BillettyperMedPris(
    val typer: List<Billettype>,
    val pris: Int,
) : Comparable<BillettyperMedPris> {
    constructor(Billettype: Billettype, pris: Int) : this(listOf(Billettype), pris)

    override fun compareTo(other: BillettyperMedPris): Int = pris.compareTo(other.pris)

    operator fun plus(other: BillettyperMedPris): BillettyperMedPris = BillettyperMedPris(typer + other.typer, pris + other.pris)

    companion object {
        fun tom() = BillettyperMedPris(emptyList(), 0)
    }
}

enum class Billettype {
    ENKELTBILLETT,
    SYVDAGERSBILLETT,
    TRETTIDAGERSBILLETT,
}

private fun finnReisedagerPerUke(grunnlag: Beregningsgrunnlag): Map<Uke, PeriodeMedDager> =
    grunnlag.splitPerUke { fom, tom ->
        val uke = Datoperiode(fom, tom)
        val dagerIPerioden = finnAntallDagerISnittetMellomUkeOgVedtaksperioder(uke, grunnlag.vedtaksperioder)
        min(grunnlag.antallReisedagerPerUke, dagerIPerioden)
    }

/**
 * Retunerer en liste med dager hvor brukeren skal reise.
 * Dagene telles fra første dagen i perioden.
 * E.g hvis brukeren skal reise man-ons i to uker så skal brukeren reise dag 1,2,3,8,9,10
 * og listen vil se slik ut: [1,2,3,8,9,10]
 */
private fun lagReisedagerListe(reisedagerPerUke: Map<Uke, PeriodeMedDager>): List<Int> =
    reisedagerPerUke.values.flatMapIndexed { indeks, periodeMedDager ->
        val startDag = reisedagerPerUke.firstNotNullOf { it.value.fom }
        val dagerTilMandag = tellDagerTilStartenAvUke(startDag, indeks)
        // dag + 1 for å 1-indeksere tellingen av dager
        val list = List(periodeMedDager.antallDager) { dag -> dagerTilMandag + dag + 1 }
        list
    }

/**
 * Teller opp antall dager fra startDag til mandagen i en gitt uke (ukeNr).
 * For den første uken (ukeNr = 0) returneres 0.
 * For påfølgende uker beregnes antall dager til mandagen i den gitte uken.
 */
private fun tellDagerTilStartenAvUke(
    startDag: LocalDate,
    ukeNr: Int,
): Int {
    if (ukeNr == 0) return 0
    val startenAvGittUke = startDag.plusWeeks(ukeNr - 1L).with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    return ChronoUnit.DAYS.between(startDag, startenAvGittUke).toInt()
}

private fun finnReisekostnadForNyEnkeltbillett(
    gjeldendeDag: Int,
    reisekostnader: MutableList<BillettyperMedPris>,
    grunnlag: Beregningsgrunnlag,
): BillettyperMedPris? =
    grunnlag.prisEnkeltbillett?.let {
        reisekostnader[max(0, gjeldendeDag - 1)] +
            BillettyperMedPris(
                listOf(
                    Billettype.ENKELTBILLETT,
                    Billettype.ENKELTBILLETT,
                ),
                grunnlag.prisEnkeltbillett * 2,
            )
    }

private fun finnReisekostnadForNySyvdagersbillett(
    gjeldendeDag: Int,
    reisekostnader: MutableList<BillettyperMedPris>,
    grunnlag: Beregningsgrunnlag,
): BillettyperMedPris? =
    grunnlag.prisSyvdagersbillett?.let {
        reisekostnader[max(0, gjeldendeDag - 7)] +
            BillettyperMedPris(Billettype.SYVDAGERSBILLETT, grunnlag.prisSyvdagersbillett)
    }

private fun Int.skalIkkeReise(
    reisedagerListe: List<Int>,
    reisedagIndeks: Int,
): Boolean = this < reisedagerListe[reisedagIndeks]

private fun antallHverdagerIPeriodeInklusiv(
    fom: LocalDate,
    tom: LocalDate,
): Int =
    generateSequence(fom) { it.plusDays(1) }
        .takeWhile { !it.isAfter(tom) }
        .count { it.dayOfWeek.value in 1..5 }

private fun finnAntallDagerISnittetMellomUkeOgVedtaksperioder(
    uke: Datoperiode,
    vedtaksperioder: List<VedtaksperiodeGrunnlag>,
): Int =
    vedtaksperioder
        .mapNotNull { Datoperiode(it.fom, it.tom).beregnSnitt(uke) }
        .sumOf { antallHverdagerIPeriodeInklusiv(it.fom, it.tom) }
