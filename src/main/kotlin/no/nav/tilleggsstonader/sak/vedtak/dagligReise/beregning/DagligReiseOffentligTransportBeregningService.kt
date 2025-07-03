package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import BeregningsresultatOffentligTransport
import BeregningsresultatPer30Dagersperiode
import BeregningsresultatTransportmiddel
import BergningsGrunnlag
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.ReiseInformasjon
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.UtgiftOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.antallDagerIPeriodeInklusiv
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.splitPerUke
import org.apache.commons.lang3.math.NumberUtils.min
import org.springframework.stereotype.Service

@Service
class DagligReiseOffentligTransportBeregningService {
    fun beregn(utgifterOffentligTransport: UtgiftOffentligTransport): BeregningsresultatOffentligTransport =
        BeregningsresultatOffentligTransport(
            perioder =
                utgifterOffentligTransport.delTil30Dagersperioder().map { it ->
                    it.lagBeregningsresultatPer30dagersperiode()
                },
        )

    fun UtgiftOffentligTransport.lagBeregningsresultatPer30dagersperiode(): BeregningsresultatPer30Dagersperiode {
        val beregningresultatPerTransportmiddel =
            reiseInformasjon.map { reise ->
                val lavestepris =
                    finnBilligsteAlternativMedGittReiseInformasjon(
                        trettidagersperiode = Datoperiode(fom, tom),
                        reiseInformasjon = reise,
                    )

                BeregningsresultatTransportmiddel(
                    kilde = reise.kilde,
                    beløp = lavestepris,
                )
            }

        return BeregningsresultatPer30Dagersperiode(
            fom = this.fom,
            tom = this.tom,
            summertBeløp = beregningresultatPerTransportmiddel.sumOf { it -> it.beløp },
            beregningsresultatTransportmiddel = beregningresultatPerTransportmiddel,
            grunnlag =
                listOf(
                    BergningsGrunnlag(
                        fom = this.fom,
                        tom = this.tom,
                        reiseInformasjon = reiseInformasjon,
                    ),
                ),
        )
    }

    fun finnBilligsteAlternativMedGittReiseInformasjon(
        trettidagersperiode: Datoperiode,
        reiseInformasjon: ReiseInformasjon,
    ): Int {
        val prisEnkeltOg7dagersbillett =
            trettidagersperiode
                .splitPerUke { fom, tom ->
                    finnReisedagerForUke(
                        ukesperiode = Datoperiode(fom, tom),
                        trettidagersperiode = trettidagersperiode,
                        reiseInformasjon = reiseInformasjon,
                    )
                }.values
                .sumOf { uke ->
                    finnBilligsteAlternativForUke(
                        antallReisedager = uke.antallDager,
                        reiseInformasjon = reiseInformasjon,
                    )
                }

        val pris30dagersbillett = reiseInformasjon.pris30dagersbillett

        return min(prisEnkeltOg7dagersbillett, pris30dagersbillett)
    }

    fun finnReisedagerForUke(
        ukesperiode: Datoperiode,
        trettidagersperiode: Datoperiode,
        reiseInformasjon: ReiseInformasjon,
    ): Int =
        min(
            reiseInformasjon.antallReisedagerPerUke,
            antallDagerIPeriodeInklusiv(ukesperiode.fom, ukesperiode.tom),
            antallDagerIPeriodeInklusiv(trettidagersperiode.fom, trettidagersperiode.tom),
        )

    fun finnBilligsteAlternativForUke(
        antallReisedager: Int,
        reiseInformasjon: ReiseInformasjon,
    ): Int {
        val prisEnkeltbilletter = antallReisedager * reiseInformasjon.prisEnkelbillett * 2
        val pris7dagersbillett = reiseInformasjon.pris7dagersbillett
        return min(prisEnkeltbilletter, pris7dagersbillett)
    }
}
