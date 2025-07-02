package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.antallDagerIPeriodeInklusiv
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.splitPerUke
import org.apache.commons.lang3.math.NumberUtils.min
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DagligReiseOffentligTransportBeregningService {
    fun beregn(beregningsInputOffentligTransport: BeregningsInputOffentligTransport): Int {
        val prisBilligstAlternativForUke =
            Datoperiode(
                beregningsInputOffentligTransport.fom,
                beregningsInputOffentligTransport.tom,
            ).splitPerUke { fom, tom ->
                finnReisedagerForUke(fom, tom, beregningsInputOffentligTransport)
            }.values
                .sumOf { it ->
                    finnBilligsteAlternativForUke(it.antallDager, beregningsInputOffentligTransport)
                }

        val pris30dagersbillett = beregningsInputOffentligTransport.pris30dagersbillett

        return min(prisBilligstAlternativForUke, pris30dagersbillett)
    }

    fun finnReisedagerForUke(
        fom: LocalDate,
        tom: LocalDate,
        beregningsInputOffentligTransport: BeregningsInputOffentligTransport,
    ): Int =
        min(
            beregningsInputOffentligTransport.antallReisedagerPerUke,
            antallDagerIPeriodeInklusiv(fom, tom),
            antallDagerIPeriodeInklusiv(beregningsInputOffentligTransport.fom, beregningsInputOffentligTransport.tom),
        )

    fun finnBilligsteAlternativForUke(
        antallDager: Int,
        beregningsInputOffentligTransport: BeregningsInputOffentligTransport,
    ): Int {
        val prisEnkeltbilletter = antallDager * beregningsInputOffentligTransport.prisEnkelbillett * 2
        val pris7dagersbillett = beregningsInputOffentligTransport.pris7dagersbillett
        return min(prisEnkeltbilletter, pris7dagersbillett)
    }
}

data class BeregningsInputOffentligTransport(
    val fom: LocalDate,
    val tom: LocalDate,
    val antallReisedagerPerUke: Int,
    val prisEnkelbillett: Int,
    val pris30dagersbillett: Int,
    val pris7dagersbillett: Int,
)
