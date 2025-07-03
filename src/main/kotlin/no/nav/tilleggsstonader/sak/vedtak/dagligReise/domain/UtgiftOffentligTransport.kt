package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.util.inneholderUkedag
import java.time.LocalDate

data class UtgiftOffentligTransport(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val antallReisedagerPerUke: Int,
    val prisEnkelbillett: Int,
    val pris30dagersbillett: Int,
    val pris7dagersbillett: Int,
) : Periode<LocalDate> {
    fun delTil30DagersPerioder(): List<UtgiftOffentligTransport> =
        this.splitPer30DagersPerioder { fom, tom ->
            UtgiftOffentligTransport(
                fom = fom,
                tom = tom,
                antallReisedagerPerUke = this.antallReisedagerPerUke,
                prisEnkelbillett = this.prisEnkelbillett,
                pris7dagersbillett = this.pris7dagersbillett,
                pris30dagersbillett = this.pris30dagersbillett,
            )
        }
}

// TODO: Skriv tester for denne
fun <P : Periode<LocalDate>, VAL : Periode<LocalDate>> P.splitPer30DagersPerioder(
    medNyPeriode: (fom: LocalDate, tom: LocalDate) -> VAL,
): List<VAL> {
    val perioder = mutableListOf<VAL>()
    var gjeldendeFom = fom
    while (gjeldendeFom <= tom) {
        val nyTom = minOf(gjeldendeFom.plusDays(30), tom)

        val nyPeriode = medNyPeriode(gjeldendeFom, nyTom)
        if (nyPeriode.inneholderUkedag()) {
            perioder.add(nyPeriode)
        }

        gjeldendeFom = nyTom.plusDays(1)
    }
    return perioder
}
