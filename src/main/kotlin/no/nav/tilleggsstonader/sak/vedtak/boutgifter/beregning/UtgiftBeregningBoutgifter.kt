package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.time.LocalDate

data class UtgiftBeregningBoutgifter(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utgift: Int,
    val skalFåDekketFaktiskeUtgifter: Boolean,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}
