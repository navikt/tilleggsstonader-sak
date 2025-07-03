package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import BeregningsresultatOffentligTransport
import BeregningsresultatPerLøpendeMåned
import BergningsGrunnlag
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.UtgiftOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.antallDagerIPeriodeInklusiv
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.splitPerUke
import org.apache.commons.lang3.math.NumberUtils.min
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DagligReiseOffentligTransportBeregningService {
    fun beregn(utgifterOffentligTransport: List<UtgiftOffentligTransport>): BeregningsresultatOffentligTransport {
        // TODO håndtere liste med input
        val utgiftOffentligTransport = utgifterOffentligTransport.single()

        val løpendeMåneder =
            utgiftOffentligTransport
                .delTilLøpendeMåneder()
                .map { måned -> måned.finnBilligsteAlternativForMåned() }

        return BeregningsresultatOffentligTransport(
            perioder = løpendeMåneder,
        )
    }

    fun UtgiftOffentligTransport.finnBilligsteAlternativForMåned(): BeregningsresultatPerLøpendeMåned {
        val prisBilligstAlternativForUke =
            Datoperiode(
                this.fom,
                this.tom,
            ).splitPerUke { fom, tom ->
                finnReisedagerForUke(fom, tom, this)
            }.values
                .sumOf { it ->
                    finnBilligsteAlternativForUke(it.antallDager, this)
                }

        val pris30dagersbillett = this.pris30dagersbillett

        val lavestePris = min(prisBilligstAlternativForUke, pris30dagersbillett)

        return BeregningsresultatPerLøpendeMåned(
            fom = this.fom,
            tom = this.tom,
            beløp = lavestePris,
            grunnlag =
                listOf(
                    BergningsGrunnlag(
                        fom = this.fom,
                        tom = this.tom,
                        antallReisedagerPerUke = this.antallReisedagerPerUke,
                        prisEnkelbillett = this.prisEnkelbillett,
                        pris30dagersbillett = this.pris30dagersbillett,
                        pris7dagersbillett = this.pris7dagersbillett,
                    ),
                ),
        )
    }

    fun finnReisedagerForUke(
        fom: LocalDate,
        tom: LocalDate,
        beregningsInputOffentligTransport: UtgiftOffentligTransport,
    ): Int =
        min(
            beregningsInputOffentligTransport.antallReisedagerPerUke,
            antallDagerIPeriodeInklusiv(fom, tom),
            antallDagerIPeriodeInklusiv(beregningsInputOffentligTransport.fom, beregningsInputOffentligTransport.tom),
        )

    fun finnBilligsteAlternativForUke(
        antallDager: Int,
        beregningsInputOffentligTransport: UtgiftOffentligTransport,
    ): Int {
        val prisEnkeltbilletter = antallDager * beregningsInputOffentligTransport.prisEnkelbillett * 2
        val pris7dagersbillett = beregningsInputOffentligTransport.pris7dagersbillett
        return min(prisEnkeltbilletter, pris7dagersbillett)
    }
}
