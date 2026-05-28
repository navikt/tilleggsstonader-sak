package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BoutgifterPerUtgiftstype
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import java.time.LocalDate

/**
 * Det kan inntreffe at to løpende utgifter med ulike kronebeløp treffer samme løpende måned. Da vet ikke systemet hvordan utgiftene skal
 * fordeles i utbetalingsperioden, så dette stopper vi foreløpig.
 *
 * Hvis begge utgiftene uansett er over makssats, så trenger vi ikke lure på hvordan utgiftene skal fordeles, fordi makssatsen uansett nås.
 * Så den casen støtter vi.
 */
fun List<UtbetalingPeriode>.validerIngenUtbetalingsperioderOverlapperFlereLøpendeUtgifter(
    utgifter: BoutgifterPerUtgiftstype,
    finnMakssats: (LocalDate) -> MakssatsBoutgifter,
): List<UtbetalingPeriode> {
    val løpendeUtgifter =
        listOf(
            TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG,
            TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER,
        ).flatMap { type ->
            (utgifter[type] ?: emptyList()).map { type to it }
        }

    val utbetalingsperioderMedFlereUtgifter =
        mapNotNull { utbetalingsperiode ->
            val utgifterSomOverlapper = løpendeUtgifter.filter { utgift -> utgift.second.overlapper(utbetalingsperiode) }
            val makssats = finnMakssats(utbetalingsperiode.fom).beløp
            utgifterSomOverlapper
                .takeIf { it.size > 1 && !it.alleUtgifterErOverMakssats(makssats) }
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

private fun List<Pair<TypeBoutgift, UtgiftBeregningBoutgifter>>.alleUtgifterErOverMakssats(makssats: Int): Boolean =
    all { (_, utgift) -> utgift.utgift >= makssats }

private fun TypeBoutgift.mapType() =
    when (this) {
        TypeBoutgift.UTGIFTER_OVERNATTING -> error("Skal ikke mappe UTGIFTER_OVERNATTING her")
        TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG -> "en bolig"
        TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER -> "to boliger"
    }
