package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BoutgifterPerUtgiftstype
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift

fun List<UtbetalingPeriode>.validerIngenLøpendeOgMidlertidigUtgiftISammeUtbetalingsperiode(
    utgifter: BoutgifterPerUtgiftstype,
): List<UtbetalingPeriode> {
    val perioderMedBådeLøpendeOgMidlertidigUtgifter = filter { it.harLøpendeOgMidlertidigUtgift(utgifter) }

    brukerfeilHvis(perioderMedBådeLøpendeOgMidlertidigUtgifter.isNotEmpty()) {
        "Det er foreløpig ikke mulig å legge både løpende utgifter og utgifter til overnatting i samme utbetalingsperiode. " +
            "Dette gjelder perioden(e): ${
                perioderMedBådeLøpendeOgMidlertidigUtgifter.joinToString(", ") {
                    it.formatertPeriodeNorskFormat()
                }
            }."
    }

    return this
}

fun UtbetalingPeriode.harLøpendeOgMidlertidigUtgift(utgifter: BoutgifterPerUtgiftstype): Boolean {
    val harLøpendeUtgifter =
        listOf(
            TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG,
            TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER,
        ).flatMap { type -> utgifter[type].orEmpty() }
            .any { utgift -> utgift.overlapper(this) }

    val harUtgifterOvernatting =
        utgifter[TypeBoutgift.UTGIFTER_OVERNATTING]
            .orEmpty()
            .any { utgift -> utgift.overlapper(this) }

    return harLøpendeUtgifter && harUtgifterOvernatting
}
