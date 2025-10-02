package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
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

fun <P : Periode<LocalDate>> P.splitPerUkeMedHelg(): List<UkeMedAntallDager> {
    val uker = mutableListOf<UkeMedAntallDager>()

    var startOfWeek = this.fom

    while (startOfWeek <= this.tom) {
        val nærmesteSøndagFremITid = startOfWeek.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val endOfWeek: LocalDate = minOf(nærmesteSøndagFremITid, this.tom)

        uker.add(
            UkeMedAntallDager(
                fom = startOfWeek,
                tom = endOfWeek,
                antallHverdager = antallHverdagerIPeriodeInklusiv(fom = startOfWeek, tom = endOfWeek),
                antallHelgedager = antallHelgedagerIPeriodeInklusiv(fom = startOfWeek, tom = endOfWeek),
            ),
        )

        startOfWeek = endOfWeek.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    }

    return uker
}

data class UkeMedAntallDager(
    override val fom: LocalDate,
    override val tom: LocalDate,
    var antallHverdager: Int,
    var antallHelgedager: Int,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}
