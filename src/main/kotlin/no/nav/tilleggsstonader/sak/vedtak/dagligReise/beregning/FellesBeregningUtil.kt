package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
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

fun <P : Periode<LocalDate>> P.splitPerUkeMedHelg(): List<PeriodeMedAntallDager> {
    val uker = mutableListOf<PeriodeMedAntallDager>()

    var startOfWeek = this.fom

    while (startOfWeek <= this.tom) {
        val nærmesteSøndagFremITid = startOfWeek.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val endOfWeek: LocalDate = minOf(nærmesteSøndagFremITid, this.tom)

        uker.add(
            PeriodeMedAntallDager(
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

    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): PeriodeMedAntallDager = this.copy(fom = fom, tom = tom)
}
