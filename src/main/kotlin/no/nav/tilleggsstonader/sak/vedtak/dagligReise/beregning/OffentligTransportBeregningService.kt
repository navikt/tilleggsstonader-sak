package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.UtgiftOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import org.springframework.stereotype.Service

@Service
class OffentligTransportBeregningService(
    private val vedtakRepository: VedtakRepository,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
) {
    fun beregn(
        vedtaksperioder: List<Vedtaksperiode>,
        oppfylteVilkår: List<VilkårDagligReise>,
        behandling: Saksbehandling,
    ): BeregningsresultatOffentligTransport {
        val utgifter =
            oppfylteVilkår
                .map { it.tilUtgiftOffentligTransport() }

        return BeregningsresultatOffentligTransport(
            reiser =
                utgifter.map { reise ->
                    beregnForReise(reise, vedtaksperioder, behandling)
                },
        )
    }

    private fun beregnForReise(
        reise: UtgiftOffentligTransport,
        vedtaksperioder: List<Vedtaksperiode>,
        behandling: Saksbehandling,
    ): BeregningsresultatForReise {
        val (justerteVedtaksperioder, justertReiseperiode) =
            finnSnittMellomReiseOgVedtaksperioder(
                reise,
                vedtaksperioder,
            )

        val trettidagerReisePerioder = justertReiseperiode.delTil30Dagersperioder()

        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(
                behandlingId = behandling.id,
                vedtaksperioder = vedtaksperioder,
            )
        val forrigeVedtak = hentForrigeVedtak(behandling)

        return if (forrigeVedtak != null) {
            brukerfeilHvis(tidligsteEndring == null) {
                "Kan ikke beregne ytelse fordi det ikke er gjort noen endringer i revurderingen"
            }

            val perioderSomSkalBeregnes =
                trettidagerReisePerioder
                    .filter { it.tom > tidligsteEndring }
                    .map { trettidagerReiseperiode ->
                        beregnForTrettiDagersPeriode(trettidagerReiseperiode, justerteVedtaksperioder)
                    }

            return BeregningsresultatForReise(
                perioder = perioderSomSkalBeregnes,
            )
        } else {
            BeregningsresultatForReise(
                perioder =
                    trettidagerReisePerioder.map { trettidagerReiseperiode ->
                        beregnForTrettiDagersPeriode(trettidagerReiseperiode, justerteVedtaksperioder)
                    },
            )
        }
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
            BeregningsgrunnlagOffentligTransport(
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
            beløp = finnBilligsteAlternativForTrettidagersPeriode(grunnlag).billigsteBelop,
            billettdetaljer = finnBilligsteAlternativForTrettidagersPeriode(grunnlag).billettyper,
        )
    }

    private fun VilkårDagligReise.tilUtgiftOffentligTransport(): UtgiftOffentligTransport {
        feilHvis(this.fakta !is FaktaOffentligTransport) {
            "Forventer kun å få inn vilkår med fakta som er av type offentlig transport ved beregning av offentlig transport"
        }

        return UtgiftOffentligTransport(
            fom = this.fom,
            tom = this.tom,
            antallReisedagerPerUke = this.fakta.reisedagerPerUke,
            prisEnkelbillett = this.fakta.prisEnkelbillett,
            prisSyvdagersbillett = this.fakta.prisSyvdagersbillett,
            pris30dagersbillett = this.fakta.prisTrettidagersbillett,
        )
    }

    private fun hentVedtak(behandlingId: BehandlingId) =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørDagligReise>()

    private fun hentForrigeVedtak(behandling: Saksbehandling): InnvilgelseEllerOpphørDagligReise? =
        behandling.forrigeIverksatteBehandlingId?.let { hentVedtak(it) }?.data
}
