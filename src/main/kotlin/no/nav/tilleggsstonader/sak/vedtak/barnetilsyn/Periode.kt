package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import java.time.temporal.Temporal

// TODO flytt denne periode

interface Periode<T> where T : Comparable<T>, T : Temporal {

    val fom: T
    val tom: T
    fun validatePeriode() {
        require(tom >= fom) { "Til-og-med før fra-og-med: $fom > $tom" }
    }
}

interface FomPeriode<T> where T : Comparable<T>, T : Temporal {
    val fom: T
}