package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.sak.util.lørdagEllerSøndag
import java.time.LocalDate

object LæremidlerPeriodeUtil {

    /**
     * Splitter en periode i løpende måneder. Løpende måned er fra dagens dato og en måned frem i tiden.
     * eks 05.01.2024-29.02.24 blir listOf( P(fom=05.01.2024,tom=04.02.2024), P(fom=05.02.2024,tom=29.02.2024) )
     */
    fun <P : Periode<LocalDate>> P.splitPerLøpendeMåneder(medNyPeriode: (fom: LocalDate, tom: LocalDate) -> P): List<P> {
        val perioder = mutableListOf<P>()
        var gjeldendeFom = fom
        while (gjeldendeFom <= tom) {
            val nyTom = minOf(gjeldendeFom.sisteDagenILøpendeMåned(), tom)

            val nyPeriode = medNyPeriode(gjeldendeFom, nyTom)
            if (nyPeriode.harDatoerIUkedager()) {
                perioder.add(nyPeriode)
            }

            gjeldendeFom = nyTom.plusDays(1)
        }
        return perioder
    }

    fun LocalDate.sisteDagenILøpendeMåned(): LocalDate =
        this.plusMonths(1).minusDays(1)

    private fun <P : Periode<LocalDate>> P.harDatoerIUkedager(): Boolean = this.alleDatoer()
        .any { dato -> !dato.lørdagEllerSøndag() }
}
