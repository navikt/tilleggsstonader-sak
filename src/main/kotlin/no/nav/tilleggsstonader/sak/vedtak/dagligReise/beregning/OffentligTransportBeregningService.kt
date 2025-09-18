package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.UtgiftOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import org.springframework.stereotype.Service

@Service
class OffentligTransportBeregningService {
    fun beregn(
        vedtaksperioder: List<Vedtaksperiode>,
        oppfylteVilkår: List<Vilkår>,
    ): Beregningsresultat {
        val utgifter =
            oppfylteVilkår
                .filter { it.offentligTransport != null }
                .map { vilkår ->
                    UtgiftOffentligTransport(
                        fom = vilkår.fom!!,
                        tom = vilkår.tom!!,
                        antallReisedagerPerUke = vilkår.offentligTransport?.reisedagerPerUke!!,
                        prisEnkelbillett = vilkår.offentligTransport.prisEnkelbillett,
                        prisSyvdagersbillett = vilkår.offentligTransport.prisSyvdagersbillett,
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
        val (justerteVedtaksperioder, justertReiseperiode) =
            finnSnittMellomReiseOgVedtaksperioder(
                reise,
                vedtaksperioder,
            )

        val trettidagerReisePerioder = justertReiseperiode.delTil30Dagersperioder()

        return BeregningsresultatForReise(
            perioder =
                trettidagerReisePerioder.map { trettidagerReiseperiode ->
                    beregnForTrettiDagersPeriode(trettidagerReiseperiode, justerteVedtaksperioder)
                },
        )
    }

    private fun beregnForTrettiDagersPeriode(
        trettidagerReisePeriode: UtgiftOffentligTransport,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatForPeriode {
        val vedtaksperiodeGrunnlag =
            finnSnittMellomReiseOgVedtaksperioder(trettidagerReisePeriode, vedtaksperioder)
                .justerteVedtaksperioder
                .map { vedtaksperiode ->
                    VedtaksperiodeGrunnlag(
                        vedtaksperiode = vedtaksperiode,
                        antallReisedager =
                            finnReisedagerIPeriode(
                                vedtaksperiode,
                                trettidagerReisePeriode.antallReisedagerPerUke,
                            ),
                    )
                }

        val grunnlag =
            Beregningsgrunnlag(
                fom = trettidagerReisePeriode.fom,
                tom = trettidagerReisePeriode.tom,
                antallReisedagerPerUke = trettidagerReisePeriode.antallReisedagerPerUke,
                prisEnkeltbillett = trettidagerReisePeriode.prisEnkelbillett,
                prisSyvdagersbillett = trettidagerReisePeriode.prisSyvdagersbillett,
                pris30dagersbillett = trettidagerReisePeriode.pris30dagersbillett,
                antallReisedager = vedtaksperiodeGrunnlag.sumOf { it.antallReisedagerIVedtaksperioden },
                vedtaksperioder = vedtaksperiodeGrunnlag,
            )

        return BeregningsresultatForPeriode(
            grunnlag = grunnlag,
            beløp = finnBilligsteAlternativForTrettidagersPeriode(grunnlag),
        )
    }
}
