package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.util.inneholderUkedag
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.time.LocalDate

data class UtgiftOffentligTransport(
    val reiseId: ReiseId,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val antallReisedagerPerUke: Int,
    val prisEnkelbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val pris30dagersbillett: Int?,
) : Periode<LocalDate>,
    KopierPeriode<UtgiftOffentligTransport> {
    fun delTil30Dagersperioder(): List<UtgiftOffentligTransport> =
        this.splitPer30DagersPerioder { fom, tom ->
            UtgiftOffentligTransport(
                reiseId = reiseId,
                fom = fom,
                tom = tom,
                antallReisedagerPerUke = antallReisedagerPerUke,
                prisEnkelbillett = prisEnkelbillett,
                prisSyvdagersbillett = prisSyvdagersbillett,
                pris30dagersbillett = pris30dagersbillett,
            )
        }

    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): UtgiftOffentligTransport = this.copy(fom = fom, tom = tom)
}

fun <P : Periode<LocalDate>, VAL : Periode<LocalDate>> P.splitPer30DagersPerioder(
    medNyPeriode: (fom: LocalDate, tom: LocalDate) -> VAL,
): List<VAL> {
    val perioder = mutableListOf<VAL>()
    var gjeldendeFom = fom
    while (gjeldendeFom <= tom) {
        // Legger kun til 29 dager fordi nåværende dag teller med
        val nyTom = minOf(gjeldendeFom.plusDays(29), tom)

        val nyPeriode = medNyPeriode(gjeldendeFom, nyTom)
        if (nyPeriode.inneholderUkedag()) {
            perioder.add(nyPeriode)
        }

        gjeldendeFom = nyTom.plusDays(1)
    }
    return perioder
}
