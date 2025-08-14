package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BoutgifterPerUtgiftstype
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import kotlin.collections.plus

fun List<UtbetalingPeriode>.validerIngenUtbetalingsperioderOverlapperFlereLøpendeUtgifter(
    utgifter: BoutgifterPerUtgiftstype,
): List<UtbetalingPeriode> {
    val fasteUtgifter =
        (utgifter[TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG] ?: emptyList()) +
            (utgifter[TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER] ?: emptyList())

    val detFinnesUtbetalingsperioderSomOverlapperFlereLøpendeUtgifter =
        any { utbetalingsperiode ->
            fasteUtgifter.count { utbetalingsperiode.overlapper(it) } > 1
        }

    feilHvis(detFinnesUtbetalingsperioderSomOverlapperFlereLøpendeUtgifter) {
        """
        Vi støtter foreløpig ikke at utbetalingsperioder overlapper mer enn én løpende utgift. 
        Utbetalingsperioder for denne behandlingen er: ${map { it.formatertPeriodeNorskFormat() }}, 
        mens utgiftsperiodene er: ${fasteUtgifter.map { it.formatertPeriodeNorskFormat() }}
        """.trimIndent()
    }

    return this
}
