package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.util.DatoFormat.DATE_FORMAT_NORSK
import no.nav.tilleggsstonader.sak.util.DatoUtil.dagensDato
import no.nav.tilleggsstonader.sak.util.DatoUtil.dagensDatoMedTid
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object DatoFormat {
    val DATE_FORMAT_ISO_YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM")
    val YEAR_MONTH_FORMAT_NORSK = DateTimeFormatter.ofPattern("MM.yyyy")
    val DATE_FORMAT_NORSK = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val GOSYS_DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy' 'HH:mm")
}

object DatoUtil {
    fun dagensDatoMedTid(): LocalDateTime = osloNow()

    fun dagensDato(): LocalDate = LocalDate.now()

    fun inneværendeÅr() = LocalDate.now().year

    fun årMånedNå() = YearMonth.now()
}

val YEAR_MONTH_MIN = YearMonth.from(LocalDate.MIN)
val YEAR_MONTH_MAX = YearMonth.from(LocalDate.MAX)

fun antallÅrSiden(dato: LocalDate?) = dato?.let { Period.between(it, dagensDato()).years }

fun LocalDate.norskFormat() = this.format(DATE_FORMAT_NORSK)

fun datoEllerIdag(localDate: LocalDate?): LocalDate = localDate ?: LocalDate.now()

fun min(
    first: LocalDateTime?,
    second: LocalDateTime?,
): LocalDateTime? =
    when {
        first == null -> second
        second == null -> first
        else -> minOf(first, second)
    }

fun min(
    first: LocalDate?,
    second: LocalDate?,
): LocalDate? =
    when {
        first == null -> second
        second == null -> first
        else -> minOf(first, second)
    }

fun max(
    first: LocalDate?,
    second: LocalDate?,
): LocalDate? =
    when {
        first == null -> second
        second == null -> first
        else -> maxOf(first, second)
    }

fun LocalDate.isEqualOrBefore(other: LocalDate) = this == other || this.isBefore(other)

fun LocalDate.isEqualOrAfter(other: LocalDate) = this == other || this.isAfter(other)

fun LocalDate.harPåfølgendeMåned(påfølgende: LocalDate): Boolean = YearMonth.from(this).erPåfølgende(YearMonth.from(påfølgende))

fun YearMonth.erPåfølgende(påfølgende: YearMonth): Boolean = this.plusMonths(1) == påfølgende

fun LocalDate.er6MndEllerMer(): Boolean = this.plusDays(183) <= LocalDate.now()

fun LocalDate.erEttÅrEllerMer(): Boolean = this.plusYears(1) <= LocalDate.now()

fun LocalDate.er6MndEllerMerOgInnenforCutoff(numberOfDaysCutoff: Long): Boolean =
    this.er6MndEllerMer() &&
        LocalDate.now() < this.plusDays(182).plusDays(numberOfDaysCutoff)

fun LocalDate.erEttÅrEllerMerOgInnenforCutoff(numberOfDaysCutoff: Long): Boolean =
    erEttÅrEllerMer() &&
        LocalDate.now() <= this.plusYears(1).plusDays(numberOfDaysCutoff)

fun LocalDateTime.harGåttAntallTimer(timer: Int) = this.plusHours(timer.toLong()) < osloNow()

fun dagensDatoMedTidNorskFormat(): String = dagensDatoMedTid().medGosysTid()

fun LocalDateTime.medGosysTid(): String = this.format(DatoFormat.GOSYS_DATE_TIME)

fun LocalDate.erLørdagEllerSøndag() = this.dayOfWeek == DayOfWeek.SATURDAY || this.dayOfWeek == DayOfWeek.SUNDAY

fun LocalDate.datoEllerNesteMandagHvisLørdagEllerSøndag() =
    if (this.erLørdagEllerSøndag()) {
        with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    } else {
        this
    }

fun LocalDate.toYearMonth(): YearMonth = YearMonth.from(this)

fun LocalDate.erFørsteDagIMåneden() = this.dayOfMonth == 1

fun LocalDate.erSisteDagIMåneden() = this.dayOfMonth == YearMonth.from(this).atEndOfMonth().dayOfMonth

fun LocalDate.tilFørsteDagIMåneden() = YearMonth.from(this).atDay(1)

fun LocalDate.tilSisteDagIMåneden() = YearMonth.from(this).atEndOfMonth()

fun LocalDate.tilSisteDagenIÅret() = this.toYearMonth().withMonth(12).atEndOfMonth()

fun LocalDate.lørdagEllerSøndag() = this.dayOfWeek == DayOfWeek.SATURDAY || this.dayOfWeek == DayOfWeek.SUNDAY

fun Periode<LocalDate>.formatertPeriodeNorskFormat() = "${this.fom.norskFormat()}–${this.tom.norskFormat()}"

fun Periode<LocalDate>.inneholderUkedag() = this.alleDatoer().any { !it.lørdagEllerSøndag() }

/**
 * https://ts-docs.ansatt.dev.nav.no/Saksbehandling/Beregning%20av%20st%C3%B8nadsbel%C3%B8p/l%C3%B8pende-m%C3%A5neder
 * En løpende måned defineres som perioden fra en gitt startdato og én måned frem i tid.
 * Hvis startdatoen er 15. januar, varer perioden til og med 14. februar.
 * Hvis startmåneden har flere dager enn måneden som følger, brukes den nest siste dagen i den påfølgende måneden.
 * Dette gjør at neste periode kan starte siste dagen i den påfølgende måneden.
 *
 * For eksempel vil en løpende måned som starter 31. januar avsluttes 27. februar,
 * siden februar ikke har en 31. dag. Neste påfølgende måned vil gå fra 28. februar til 27. mars.
 */
fun LocalDate.sisteDagenILøpendeMåned(): LocalDate = this.plusMonths(1).minusDays(1)
