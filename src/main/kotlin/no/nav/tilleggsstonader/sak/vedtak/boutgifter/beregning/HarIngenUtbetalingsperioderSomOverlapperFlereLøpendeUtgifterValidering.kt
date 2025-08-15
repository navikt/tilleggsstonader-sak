package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BoutgifterPerUtgiftstype
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift

fun List<UtbetalingPeriode>.validerIngenUtbetalingsperioderOverlapperFlereLøpendeUtgifter(
    utgifter: BoutgifterPerUtgiftstype,
): List<UtbetalingPeriode> {
    val fasteUtgifter =
        listOf(
            TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG,
            TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER,
        ).flatMap { type ->
            (utgifter[type] ?: emptyList()).map { type to it }
        }

    val utbetalingsperioderMedFlereUtgifter =
        mapNotNull { utbetalingsperiode ->
            val utgifterSomOverlapper = fasteUtgifter.filter { utgift -> utgift.second.overlapper(utbetalingsperiode) }
            utgifterSomOverlapper
                .takeIf { it.size > 1 }
                ?.let { utbetalingsperiode to it }
        }

    brukerfeilHvis(utbetalingsperioderMedFlereUtgifter.isNotEmpty()) {
        buildString {
            appendLine("Vi støtter foreløpig ikke at utbetalingsperioder inneholder mer enn én løpende utgift.")
            appendLine("Utbetalingsperioder som inneholder flere løpende utgifter:")
            appendLine()
            utbetalingsperioderMedFlereUtgifter.forEachIndexed { index, (utbetalingsperiode, utgifter) ->
                appendLine("${utbetalingsperiode.formatertPeriodeNorskFormat()} inneholder utgiftene:")
                utgifter.forEachIndexed { utgiftIndex, (type, utgift) ->
                    append("  - ${utgift.formatertPeriodeNorskFormat()} - ${type.mapType()} (${utgift.utgift} kr)")
                    if (utgiftIndex + 1 < utgifter.size) {
                        appendLine()
                    }
                }
                if (index + 1 < utbetalingsperioderMedFlereUtgifter.size) {
                    appendLine()
                    appendLine()
                }
            }
        }
    }

    return this
}

private fun TypeBoutgift.mapType() =
    when (this) {
        TypeBoutgift.UTGIFTER_OVERNATTING -> error("Skal ikke mappe UTGIFTER_OVERNATTING her")
        TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG -> "en bolig"
        TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER -> "to boliger"
    }
