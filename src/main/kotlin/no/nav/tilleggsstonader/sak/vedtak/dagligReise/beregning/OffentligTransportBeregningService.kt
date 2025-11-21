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
import java.time.LocalDate

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

        val forrigeVedtak = hentForrigeVedtak(behandling)

        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(
                behandlingId = behandling.id,
                vedtaksperioder = vedtaksperioder,
            )

        return if (forrigeVedtak != null) {
            brukerfeilHvis(tidligsteEndring == null) {
                "Kan ikke beregne ytelse fordi det ikke er gjort noen endringer i revurderingen"
            }

            settSammenGamleOgNyePerioder(
                tidligsteEndring = tidligsteEndring,
                nyttBeregningsresultat =
                    BeregningsresultatForReise(
                        perioder =
                            trettidagerReisePerioder.map { trettidagerReiseperiode ->
                                beregnForTrettiDagersPeriode(trettidagerReiseperiode, justerteVedtaksperioder)
                            },
                    ),
                forrigeBeregningsresultat =
                    forrigeVedtak.beregningsresultat.offentligTransport!!
                        .reiser,
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

    /**
     * Slår sammen perioder fra forrige og nytt vedtak.
     * Beholder perioder fra forrige vedtak frem til tidligsteEndring-datoen.
     * Bruker reberegnede perioder fra og med tidligsteEndring-datoen
     * Dette gjøres for at vi ikke skal reberegne perioder som ikke er med i revurderingen, i tilfelle beregningskoden har endret seg siden sist.
     * Vi trenger derimot å reberegne alle perioder som ligger etter tidligsteEndring-datoen, da utgiftene, antall samlinger osv kan ha endret seg.
     */
    private fun settSammenGamleOgNyePerioder(
        tidligsteEndring: LocalDate,
        nyttBeregningsresultat: BeregningsresultatForReise,
        forrigeBeregningsresultat: List<BeregningsresultatForReise>,
    ): BeregningsresultatForReise {
        if (LocalDate.now() == tidligsteEndring) {
            error("tidligste endring kan ikke være det samme som dagens dato, det kan bli krøll")
        }

        val beregnFraTidligsteEndring =
            forrigeBeregningsresultat
                .flatMap { it.perioder }
                .sortedBy { it.grunnlag.fom }
                .firstOrNull { periode ->
                    tidligsteEndring in periode.grunnlag.fom..periode.grunnlag.tom
                }?.grunnlag
                ?.fom
                ?: tidligsteEndring

        val perioderFraForrigeVedtakSomSkalBeholdes =
            forrigeBeregningsresultat
                .flatMap { it.perioder }
                .filter { it.grunnlag.tom < beregnFraTidligsteEndring }

        val reberegnedePerioder =
            nyttBeregningsresultat.perioder
                .filter { it.grunnlag.tom >= beregnFraTidligsteEndring }

        return BeregningsresultatForReise(
            perioder = perioderFraForrigeVedtakSomSkalBeholdes + reberegnedePerioder,
        )
    }

    private fun hentVedtak(behandlingId: BehandlingId) =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørDagligReise>()

    private fun hentForrigeVedtak(behandling: Saksbehandling): InnvilgelseEllerOpphørDagligReise? =
        behandling.forrigeIverksatteBehandlingId?.let { hentVedtak(it) }?.data

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
}
