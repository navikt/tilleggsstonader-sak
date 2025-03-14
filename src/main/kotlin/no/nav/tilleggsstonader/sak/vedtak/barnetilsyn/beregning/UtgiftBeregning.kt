package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.util.toYearMonth
import java.time.LocalDate
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

/**
 * Dersom man har en lang utgiftsperiode for 1.1 - 31.1 så skal den splittes opp fra revurderFra sånn at man får 2 perioder
 * Eks for revurderFra=15.1 så får man 1.1 - 14.1 og 15.1 - 31.1
 * Dette for å kunne filtrere vekk perioder som begynner før revurderFra og beregne beløp som skal utbetales i gitt måned
 */
fun List<UtgiftBeregning>.splitFraRevurderFra(revurderFra: LocalDate?): List<UtgiftBeregning> {
    val revurderFraMåned = revurderFra?.toYearMonth() ?: return this
    return this.flatMap {
        if (it.fom < revurderFraMåned && revurderFraMåned <= it.tom) {
            listOf(
                it.copy(tom = revurderFraMåned.minusMonths(1)),
                it.copy(fom = revurderFraMåned),
            )
        } else {
            listOf(it)
        }
    }
}
