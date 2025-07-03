package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.splitPerLøpendeMåneder
import java.time.LocalDate

data class UtgiftOffentligTransport(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val antallReisedagerPerUke: Int,
    val prisEnkelbillett: Int,
    val pris30dagersbillett: Int,
    val pris7dagersbillett: Int,
) : Periode<LocalDate> {
    fun delTilLøpendeMåneder(): List<UtgiftOffentligTransport> =
        this.splitPerLøpendeMåneder { fom, tom ->
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
