package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.Temporal

// TODO flytt denne periode

interface Periode<T> : Comparable<Periode<T>> where T : Comparable<T>, T : Temporal {

    val fom: T
    val tom: T
    fun validatePeriode() {
        require(tom >= fom) { "Til-og-med før fra-og-med: $fom > $tom" }
    }

    fun overlapper(other: Periode<T>): Boolean {
        return !(this.tom < other.fom || this.fom > other.tom)
    }

    override fun compareTo(other: Periode<T>): Int {
        return Comparator.comparing(Periode<T>::fom).thenComparing(Periode<T>::tom).compare(this, other)
    }
}

fun <T> List<Periode<T>>.erSortert(): Boolean where T : Comparable<T>, T : Temporal {
    return zipWithNext().all { it.first < it.second }
}

fun <T> List<Periode<T>>.overlapper(): Boolean where T : Comparable<T>, T : Temporal {
    return this.sortedBy { it.fom }.zipWithNext().any { it.first.overlapper(it.second) }
}

/**
 * Interface for å slå sammen 2 perioder
 */
interface Mergeable<R, T : Periode<R>> where R : Comparable<R>, R : Temporal {
    fun merge(other: T): T
}

/**
 * Forventer at perioder er sorterte når man slår de sammen
 */
fun <T, P> List<P>.mergeSammenhengende(skalMerges: (P, P) -> Boolean): List<P>
        where P : Periode<T>, T : Comparable<T>, T : Temporal, P : Mergeable<T, P> {
    return this.fold(mutableListOf()) { acc, entry ->
        val last = acc.lastOrNull()
        if (last != null && skalMerges(last, entry)) {
            acc.removeLast()
            acc.add(last.merge(entry))
        } else {
            acc.add(entry)
        }
        acc
    }
}

/**
 * Splitter en datoperiode till verdi per måned,
 * eks 05.01.2023 - 08.02.2023 blir listOf(Pair(jan, verdi), Pair(feb, verdi))
 */
fun <P : Periode<LocalDate>, VAL> P.splitPerMåned(value: (P) -> VAL): List<Pair<YearMonth, VAL>> {
    val perioder = mutableListOf<Pair<YearMonth, VAL>>()
    var dato = fom
    val verdi = value(this)
    while (dato <= this.tom) {
        val årMåned = YearMonth.from(dato)
        perioder.add(Pair(årMåned, verdi))
        dato = årMåned.atEndOfMonth().plusDays(1)
    }
    return perioder
}

/**
 * Splitter en månedsperiode till verdi per måned,
 * eks 01.2023 - 02.2023 blir listOf(Pair(jan, verdi), Pair(feb, verdi))
 */
@JvmName("yearMonthSplitPerMåned")
fun <P : Periode<YearMonth>, VAL> P.splitPerMåned(value: (P) -> VAL): List<Pair<YearMonth, VAL>> {
    val perioder = mutableListOf<Pair<YearMonth, VAL>>()
    var dato = fom
    val verdi = value(this)
    while (dato <= this.tom) {
        perioder.add(Pair(dato, verdi))
        dato = dato.plusMonths(1)
    }
    return perioder
}
