package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

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