package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import BeregningsresultatOffentligTransport
import BeregningsresultatPerMåned
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

        val prisBilligstAlternativForUke =
            Datoperiode(
                utgiftOffentligTransport.fom,
                utgiftOffentligTransport.tom,
            ).splitPerUke { fom, tom ->
                finnReisedagerForUke(fom, tom, utgiftOffentligTransport)
            }.values
                .sumOf { it ->
                    finnBilligsteAlternativForUke(it.antallDager, utgiftOffentligTransport)
                }

        val pris30dagersbillett = utgiftOffentligTransport.pris30dagersbillett

        val lavestePris = min(prisBilligstAlternativForUke, pris30dagersbillett)

        // TODO håndtere flere måneder
        // Hvis bruker søker for jan til mai må vi finne ut hvor mye søker skal ha per måned
        // altså per jan, feb, mars osv
        val beregingsresultatPerMåned =
            BeregningsresultatPerMåned(
                fom = utgiftOffentligTransport.fom,
                tom = utgiftOffentligTransport.tom,
                beløp = lavestePris,
                grunnlag =
                    listOf(
                        BergningsGrunnlag(
                            fom = utgiftOffentligTransport.fom,
                            tom = utgiftOffentligTransport.tom,
                            antallReisedagerPerUke = utgiftOffentligTransport.antallReisedagerPerUke,
                            prisEnkelbillett = utgiftOffentligTransport.prisEnkelbillett,
                            pris30dagersbillett = utgiftOffentligTransport.pris30dagersbillett,
                            pris7dagersbillett = utgiftOffentligTransport.pris7dagersbillett,
                        ),
                    ),
            )

        return BeregningsresultatOffentligTransport(
            peroder = listOf(beregingsresultatPerMåned),
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
