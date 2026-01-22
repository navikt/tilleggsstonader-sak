package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

fun antallHverdagerIPeriodeInklusiv(
    fom: LocalDate,
    tom: LocalDate,
): Int =
    generateSequence(fom) { it.plusDays(1) }
        .takeWhile { !it.isAfter(tom) }
        .count { it.dayOfWeek.value in 1..5 }

fun antallHelgedagerIPeriodeInklusiv(
    fom: LocalDate,
    tom: LocalDate,
): Int =
    generateSequence(fom) { it.plusDays(1) }
        .takeWhile { !it.isAfter(tom) }
        .count { it.dayOfWeek.value in 6..7 }

fun <P : Periode<LocalDate>> P.splitPerUkeMedHelg(): List<Datoperiode> {
    val uker = mutableListOf<Datoperiode>()

    var startOfWeek = this.fom

    while (startOfWeek <= this.tom) {
        val nærmesteSøndagFremITid = startOfWeek.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val endOfWeek: LocalDate = minOf(nærmesteSøndagFremITid, this.tom)

        uker.add(
            Datoperiode(
                fom = startOfWeek,
                tom = endOfWeek,
            ),
        )

        startOfWeek = endOfWeek.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    }

    return uker
}

data class PeriodeMedAntallDager(
    override val fom: LocalDate,
    override val tom: LocalDate,
    var antallHverdager: Int,
    var antallHelgedager: Int,
) : Periode<LocalDate>,
    KopierPeriode<PeriodeMedAntallDager> {
    init {
        validatePeriode()
    }

    constructor(fom: LocalDate, tom: LocalDate) : this(
        fom = fom,
        tom = tom,
        antallHverdager = antallHverdagerIPeriodeInklusiv(fom = fom, tom = tom),
        antallHelgedager = antallHelgedagerIPeriodeInklusiv(fom = fom, tom = tom),
    )

    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): PeriodeMedAntallDager = this.copy(fom = fom, tom = tom)
}

data class ReiseOgVedtaksperioderSnitt<P>(
    val justerteVedtaksperioder: List<Vedtaksperiode>,
    val justertReiseperiode: P?,
) where P : Periode<LocalDate>, P : KopierPeriode<P>

fun <P> finnSnittMellomReiseOgVedtaksperioder(
    reise: P,
    vedtaksperioder: List<Vedtaksperiode>,
): ReiseOgVedtaksperioderSnitt<P> where P : Periode<LocalDate>, P : KopierPeriode<P> {
    val justerteVedtaksperioder = vedtaksperioder.mapNotNull { it.beregnSnitt(reise) }.sorted()

    if (justerteVedtaksperioder.isEmpty()) {
        return ReiseOgVedtaksperioderSnitt(
            justerteVedtaksperioder = emptyList(),
            justertReiseperiode = null,
        )
    }

    return ReiseOgVedtaksperioderSnitt(
        justerteVedtaksperioder = justerteVedtaksperioder,
        justertReiseperiode =
            reise.medPeriode(
                fom = maxOf(justerteVedtaksperioder.first().fom, reise.fom),
                tom = minOf(justerteVedtaksperioder.last().tom, reise.tom),
            ),
    )
}
