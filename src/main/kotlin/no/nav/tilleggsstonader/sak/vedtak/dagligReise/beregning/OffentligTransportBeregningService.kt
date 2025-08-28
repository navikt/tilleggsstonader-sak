package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.UtgiftOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.antallDagerIPeriodeInklusiv
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.splitPerUke
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import org.springframework.stereotype.Service
import kotlin.math.min

@Service
class OffentligTransportBeregningService(
    private val vilkårService: VilkårService,
) {
    fun beregn(
        behandlingId: BehandlingId,
        vedtaksperioder: List<Vedtaksperiode>,
    ): Beregningsresultat {
        val oppfylteVilkår = vilkårService.hentOppfylteDagligReiseVilkår(behandlingId)
        val utgifter =
            oppfylteVilkår
                .filter { it.offentligTransport != null }
                .map { vilkår ->
                    UtgiftOffentligTransport(
                        fom = vilkår.fom!!,
                        tom = vilkår.tom!!,
                        antallReisedagerPerUke = vilkår.offentligTransport?.reisedagerPerUke!!,
                        prisEnkelbillett = vilkår.offentligTransport.prisEnkelbillett,
                        pris30dagersbillett = vilkår.offentligTransport.prisTrettidagersbillett,
                    )
                }

        return Beregningsresultat(
            reiser =
                utgifter.map { reise ->
                    beregnForReise(reise, vedtaksperioder)
                },
        )
    }

    private fun beregnForReise(
        reise: UtgiftOffentligTransport,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatForReise {
        val justerteVedtaksperioder = finnSnittForVedtaksperioder(reise, vedtaksperioder)
        val justertReiseperiode = finnSnittForReiseperioder(reise, justerteVedtaksperioder)

        val trettidagersperioder = justertReiseperiode.delTil30Dagersperioder()

        return BeregningsresultatForReise(
            perioder =
                trettidagersperioder.map { periode ->
                    beregnForPeriode(periode, justerteVedtaksperioder)
                },
        )
    }

    private fun finnSnittForReiseperioder(
        reise: UtgiftOffentligTransport,
        vedtaksperioder: List<Vedtaksperiode>,
    ) = reise.copy(
        fom = maxOf(vedtaksperioder.first().fom, reise.fom),
        tom = minOf(vedtaksperioder.last().tom, reise.tom),
    )

    private fun finnSnittForVedtaksperioder(
        reise: UtgiftOffentligTransport,
        vedtaksperioder: List<Vedtaksperiode>,
    ): List<Vedtaksperiode> =
        vedtaksperioder
            .mapNotNull { it.beregnSnitt(reise) }

    private fun beregnForPeriode(
        periode: UtgiftOffentligTransport,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatForPeriode {
        val vedtaksperiodeGrunnlag =
            finnSnittForVedtaksperioder(periode, vedtaksperioder)
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
                min(antallReisedagerPerUke, antallDagerIPeriodeInklusiv(fom, tom))
            }.values
            .sumOf { it.antallDager }

    private fun finnBilligsteAlternativ(grunnlag: Beregningsgrunnlag): Int {
        val prisEnkeltbilletter = grunnlag.antallReisedager * grunnlag.prisEnkeltbillett * 2
        val pris30dagersbillett = grunnlag.pris30dagersbillett

        return min(prisEnkeltbilletter, pris30dagersbillett)
    }
}
