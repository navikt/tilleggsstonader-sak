package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.Temporal

sealed class UtgiftBeregningType<T>(
    override val fom: T,
    override val tom: T,
    open val utgift: Int,
) : Periode<T> where T : Comparable<T>, T : Temporal

data class UtgiftBeregningMåned(
    override val fom: YearMonth,
    override val tom: YearMonth,
    override val utgift: Int,
) : UtgiftBeregningType<YearMonth>(fom, tom, utgift) {
    init {
        validatePeriode()
    }
}

data class UtgiftBeregningDato(
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val utgift: Int,
) : UtgiftBeregningType<LocalDate>(fom, tom, utgift) {
    init {
        validatePeriode()
    }
}
