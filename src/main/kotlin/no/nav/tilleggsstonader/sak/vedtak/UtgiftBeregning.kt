package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.time.YearMonth

data class UtgiftBeregning(
    override val fom: YearMonth,
    override val tom: YearMonth,
    val utgift: Int,
) : Periode<YearMonth> {
    init {
        validatePeriode()
    }
}
