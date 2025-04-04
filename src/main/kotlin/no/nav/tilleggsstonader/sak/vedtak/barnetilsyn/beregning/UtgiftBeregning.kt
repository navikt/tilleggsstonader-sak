package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.time.LocalDate

data class UtgiftBeregning(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utgift: Int,
) : Periode<LocalDate>,
    Mergeable<LocalDate, UtgiftBeregning> {
    init {
        validatePeriode()
    }

    override fun merge(other: UtgiftBeregning): UtgiftBeregning =
        this.copy(fom = minOf(this.fom, other.fom), tom = maxOf(this.tom, other.tom))
}
