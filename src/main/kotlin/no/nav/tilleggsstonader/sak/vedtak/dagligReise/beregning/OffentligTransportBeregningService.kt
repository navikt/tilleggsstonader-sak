package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import Beregningsgrunnlag
import Beregningsresultat
import BeregningsresultatForPeriode
import BeregningsresultatForReise
import VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.UtgiftOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.antallDagerIPeriodeInklusiv
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.splitPerUke
import org.springframework.stereotype.Service
import kotlin.math.min

@Service
class OffentligTransportBeregningService {
    fun beregn(
        utgifter: List<UtgiftOffentligTransport>,
        vedtaksperioder: List<Vedtaksperiode>,
    ): Beregningsresultat =

        Beregningsresultat(
            reiser =
                utgifter.map { reise ->
                    beregnForReise(reise, vedtaksperioder)
                },
        )

    private fun beregnForReise(
        reise: UtgiftOffentligTransport,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatForReise {
        val relevanteVedtaksperioder = finnRelevanteVedtaksperioder(reise, vedtaksperioder)

        val justertReiseperiode =
            reise.copy(
                fom = maxOf(relevanteVedtaksperioder.first().fom, reise.fom),
                tom = minOf(relevanteVedtaksperioder.last().tom, reise.tom),
            )

        val trettidagersperioder = justertReiseperiode.delTil30Dagersperioder()

        return BeregningsresultatForReise(
            perioder =
                trettidagersperioder.map { periode ->
                    beregnForPeriode(periode, relevanteVedtaksperioder)
                },
        )
    }

    private fun finnRelevanteVedtaksperioder(
        reise: UtgiftOffentligTransport,
        vedtaksperioder: List<Vedtaksperiode>,
    ): List<Vedtaksperiode> =
        vedtaksperioder
            .filter { it.overlapper(reise) }
            .map { kuttVedtaksperiodeTilOverlapp(reise, it) }

    private fun kuttVedtaksperiodeTilOverlapp(
        reise: UtgiftOffentligTransport,
        vedtaksperiode: Vedtaksperiode,
    ): Vedtaksperiode =
        vedtaksperiode.copy(
            fom = maxOf(vedtaksperiode.fom, reise.fom),
            tom = minOf(vedtaksperiode.tom, reise.tom),
        )

    private fun beregnForPeriode(
        periode: UtgiftOffentligTransport,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatForPeriode {
        val vedtaksperiodeGrunnlag =
            finnRelevanteVedtaksperioder(periode, vedtaksperioder)
                .map {
                    VedtaksperiodeGrunnlag(
                        vedtaksperiode = it,
                        antallReisedager = finnReisedagerIPeriode(it, periode.antallReisedagerPerUke),
                    )
                }

        val grunnlag =
            Beregningsgrunnlag(
                fom = periode.fom,
                tom = periode.tom,
                antallReisedagerPerUke = periode.antallReisedagerPerUke,
                prisEnkeltbillett = periode.prisEnkelbillett,
                pris30dagersbillett = periode.pris30dagersbillett,
                antallReisedager = vedtaksperiodeGrunnlag.sumOf { it.antallReisedagerIVedtaksperioden },
                vedtaksperioder = vedtaksperiodeGrunnlag,
            )

        return BeregningsresultatForPeriode(
            grunnlag = grunnlag,
            beløp = finnBilligsteAlternativ(grunnlag),
        )
    }

    private fun finnReisedagerIPeriode(
        vedtaksperiode: Vedtaksperiode,
        antallReisedagerPerUke: Int,
    ): Int =
        vedtaksperiode
            .splitPerUke { fom, tom ->
                kotlin.math.min(antallReisedagerPerUke, antallDagerIPeriodeInklusiv(fom, tom))
            }.values
            .sumOf { it.antallDager }

    private fun finnBilligsteAlternativ(grunnlag: Beregningsgrunnlag): Int {
        val prisEnkeltbilletter = grunnlag.antallReisedager * grunnlag.prisEnkeltbillett * 2
        val pris30dagersbillett = grunnlag.pris30dagersbillett

        return min(prisEnkeltbilletter, pris30dagersbillett)
    }
}
