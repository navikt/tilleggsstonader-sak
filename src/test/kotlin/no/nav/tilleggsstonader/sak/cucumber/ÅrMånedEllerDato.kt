package no.nav.tilleggsstonader.sak.cucumber

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import java.time.LocalDate
import java.time.YearMonth

data class ÅrMånedEllerDato(val dato: Any) {

    fun datoEllerFørsteDagenIMåneden(): LocalDate {
        return if (dato is LocalDate) {
            dato
        } else if (dato is YearMonth) {
            dato.atDay(1)
        } else {
            error("Typen er feil - ${dato::class.java.simpleName}")
        }
    }

    fun datoEllerSisteDagenIMåneden(): LocalDate {
        return if (dato is LocalDate) {
            dato
        } else if (dato is YearMonth) {
            dato.atEndOfMonth()
        } else {
            error("Typen er feil - ${dato::class.java.simpleName}")
        }
    }

    fun førsteDagenIMåneden(): LocalDate {
        return if (dato is LocalDate) {
            feilHvis(dato.dayOfMonth != 1) { "Må være første dagen i måneden - $dato" }
            dato
        } else if (dato is YearMonth) {
            dato.atDay(1)
        } else {
            error("Typen er feil - ${dato::class.java.simpleName}")
        }
    }

    fun sisteDagenIMåneden(): LocalDate {
        return if (dato is LocalDate) {
            feilHvis(dato != YearMonth.from(dato).atEndOfMonth()) { "Må være siste dagen i måneden - $dato" }
            dato
        } else if (dato is YearMonth) {
            dato.atEndOfMonth()
        } else {
            error("Typen er feil - ${dato::class.java.simpleName}")
        }
    }
}

fun ÅrMånedEllerDato?.førsteDagenIMånedenEllerDefault(dato: LocalDate = YearMonth.now().atDay(1)) =
    this?.førsteDagenIMåneden() ?: dato

fun ÅrMånedEllerDato?.sisteDagenIMånedenEllerDefault(dato: LocalDate = YearMonth.now().atEndOfMonth()) =
    this?.sisteDagenIMåneden() ?: dato
