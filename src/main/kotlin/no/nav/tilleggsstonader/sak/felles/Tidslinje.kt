package no.nav.tilleggsstonader.sak.felles

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.time.LocalDate

class Tidslinje<P>(
    val perioder: List<P>,
) where P : Periode<LocalDate>, P : KopierPeriode<P> {
    /**
     * Splitter tidslinja i segmenter ved oppgitte grensedatoer.
     * Perioder som strekker seg over en grensedato deles i to.
     * Tomme segmenter utelates.
     *
     * Eksempel 1 – grensedato midt i en periode:
     * - Periode: 01.01–31.03, grensedato: 01.03
     * - Segment 1: [01.01–28.02]
     * - Segment 2: [01.03–31.03]
     *
     * Eksempel 2 – to grensedatoer gir tre segmenter:
     * - Periode: 01.01–30.04, grensedatoer: 01.02, 01.04
     * - Segment 1: [01.01–31.01]
     * - Segment 2: [01.02–31.03]
     * - Segment 3: [01.04–30.04]
     *
     * Eksempel 3 – grensedato mellom to adskilte perioder:
     * - Perioder: 01.01–31.01 og 01.03–31.03, grensedato: 01.02
     * - Segment 1: [01.01–31.01]
     * - Segment 2: [01.03–31.03]
     */
    fun splittVedDatoer(splittdatoer: List<LocalDate>): List<Tidslinje<P>> {
        val segmenter = mutableListOf<Tidslinje<P>>()
        var resterende = this

        for (dato in splittdatoer) {
            val splittet = resterende.splitVedDato(dato)
            val før = splittet.filter { it.tom < dato }
            resterende = splittet.filter { it.fom >= dato }
            if (før.isNotEmpty()) segmenter.add(før)
        }

        if (resterende.isNotEmpty()) segmenter.add(resterende)
        return segmenter
    }

    fun filter(predicate: (P) -> Boolean): Tidslinje<P> = Tidslinje(perioder.filter(predicate))

    fun isNotEmpty(): Boolean = perioder.isNotEmpty()

    fun Tidslinje<P>.splitVedDato(dato: LocalDate): Tidslinje<P> =
        Tidslinje(
            perioder.flatMap { periode ->
                if (periode.fom < dato && dato <= periode.tom) {
                    listOf(
                        periode.medPeriode(fom = periode.fom, tom = dato.minusDays(1)),
                        periode.medPeriode(fom = dato, tom = periode.tom),
                    )
                } else {
                    listOf(periode)
                }
            },
        )
}
