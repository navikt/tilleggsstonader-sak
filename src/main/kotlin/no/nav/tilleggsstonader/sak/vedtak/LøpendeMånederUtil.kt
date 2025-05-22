package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.util.inneholderUkedag
import no.nav.tilleggsstonader.sak.util.sisteDagenILøpendeMåned
import java.time.LocalDate

/**
 * Splitter en periode i løpende måneder. Løpende måned er fra dagens dato og en måned frem i tiden.
 * eks 05.01.2024-29.02.24 blir listOf( P(fom=05.01.2024,tom=04.02.2024), P(fom=05.02.2024,tom=29.02.2024) )
 */
fun <P : Periode<LocalDate>, VAL : Periode<LocalDate>> P.splitPerLøpendeMåneder(
    medNyPeriode: (fom: LocalDate, tom: LocalDate) -> VAL,
): List<VAL> {
    val perioder = mutableListOf<VAL>()
    var gjeldendeFom = fom
    while (gjeldendeFom <= tom) {
        val nyTom = minOf(gjeldendeFom.sisteDagenILøpendeMåned(), tom)

        val nyPeriode = medNyPeriode(gjeldendeFom, nyTom)
        if (nyPeriode.inneholderUkedag()) {
            perioder.add(nyPeriode)
        }

        gjeldendeFom = nyTom.plusDays(1)
    }
    return perioder
}
