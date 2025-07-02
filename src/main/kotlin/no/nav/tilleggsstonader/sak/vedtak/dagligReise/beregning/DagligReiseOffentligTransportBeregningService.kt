package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.antallDagerIPeriodeInklusiv
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.splitPerUke
import org.apache.commons.lang3.math.NumberUtils.min
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DagligReiseOffentligTransportBeregningService {
    fun beregn(beregningsInputOffentligTransport: BeregningsInputOffentligTransport): Int =
        Datoperiode(beregningsInputOffentligTransport.fom, beregningsInputOffentligTransport.tom)
            .splitPerUke { fom, tom ->
                finnReisedagerForUke(fom, tom, beregningsInputOffentligTransport)
            }.values
            .sumOf { it ->
                prisKunEnkelbilett(it.antallDager, beregningsInputOffentligTransport.prisEnkelBilett)
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

    fun prisKunEnkelbilett(
        antallDager: Int,
        prisEnkelBilett: Int,
    ): Int = antallDager * prisEnkelBilett
}

data class BeregningsInputOffentligTransport(
    val fom: LocalDate,
    val tom: LocalDate,
    val antallReisedagerPerUke: Int,
    val prisEnkelBilett: Int,
//    val pris30dagersBilett: Int,
//    val pris7dagersBilett: Int,
)
