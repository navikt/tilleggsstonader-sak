package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.DagligReiseBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.AvslagDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.OpphørDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.VedtakDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DagligReiseVedtakSteg(
    private val beregningService: DagligReiseBeregningService,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
    private val vedtakRepository: VedtakRepository,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val simuleringService: SimuleringService,
    private val dagligReiseBeregningService: DagligReiseBeregningService,
) : BehandlingSteg<VedtakDagligReiseRequest> {
    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: VedtakDagligReiseRequest,
    ) {
        nullstillEksisterendeVedtakPåBehandling(saksbehandling)
        lagreVedtaksresultat(saksbehandling, data)
    }

    override fun utførOgReturnerNesteSteg(
        saksbehandling: Saksbehandling,
        data: VedtakDagligReiseRequest,
        kanBehandlePrivatBil: Boolean,
    ): StegType {
        utførSteg(saksbehandling, data)
        return finnNesteSteg(data)
    }

    fun lagreVedtaksresultat(
        saksbehandling: Saksbehandling,
        vedtak: VedtakDagligReiseRequest,
    ) {
        when (vedtak) {
            is InnvilgelseDagligReiseRequest -> lagreVedtaksperioderOgBeregn(saksbehandling, vedtak)
            is AvslagDagligReiseDto -> lagreAvslag(saksbehandling, vedtak)
            is OpphørDagligReiseRequest -> TODO("Tilpass opphør til ny flyt")
        }
    }

    private fun lagreVedtaksperioderOgBeregn(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseDagligReiseRequest,
    ) {
        val vedtaksperioder = vedtak.vedtaksperioder.tilDomene()

        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(
                saksbehandling.id,
                vedtaksperioder,
            )

        val beregningsresultatOffentligTransportOgRammePrivatBil =
            dagligReiseBeregningService.beregnOffentligTransportOgRammevedtak(
                vedtaksperioder = vedtaksperioder,
                behandling = saksbehandling,
                typeVedtak = TypeVedtak.INNVILGELSE,
                tidligsteEndring = tidligsteEndring,
            )

        lagreInnvilgetVedtak(
            behandling = saksbehandling,
            vedtaksperioder = vedtaksperioder,
            begrunnelse = vedtak.begrunnelse,
            tidligsteEndring = tidligsteEndring,
            beregningsresultat = beregningsresultatOffentligTransportOgRammePrivatBil,
        )
    }

    private fun lagreAvslag(
        saksbehandling: Saksbehandling,
        vedtak: AvslagDagligReiseDto,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.AVSLAG,
                data =
                    AvslagDagligReise(
                        årsaker = vedtak.årsakerAvslag,
                        begrunnelse = vedtak.begrunnelse,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
            ),
        )
    }

    private fun lagreInnvilgetVedtak(
        behandling: Saksbehandling,
        beregningsresultat: BeregningsresultatDagligReise,
        vedtaksperioder: List<Vedtaksperiode>,
        begrunnelse: String?,
        tidligsteEndring: LocalDate?,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = behandling.id,
                type = TypeVedtak.INNVILGELSE,
                data =
                    InnvilgelseDagligReise(
                        vedtaksperioder = vedtaksperioder,
                        begrunnelse = begrunnelse,
                        beregningsresultat = beregningsresultat,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = tidligsteEndring,
            ),
        )
    }

    private fun finnNesteSteg(vedtak: VedtakDagligReiseRequest): StegType =
        when (vedtak) {
            // TODO: Her kan man hoppe rett til beregning om man kun har registrert offentlig transport
            is InnvilgelseDagligReiseRequest -> StegType.KJØRELISTE
            is AvslagDagligReiseDto -> StegType.SEND_TIL_BESLUTTER
            is OpphørDagligReiseRequest -> TODO("Tilpass opphør til ny flyt")
        }

    private fun nullstillEksisterendeVedtakPåBehandling(saksbehandling: Saksbehandling) {
        vedtakRepository.deleteById(saksbehandling.id)
        tilkjentYtelseService.slettTilkjentYtelseForBehandling(saksbehandling)
        simuleringService.slettSimuleringForBehandling(saksbehandling)
    }

    override fun stegType(): StegType = StegType.VEDTAK
}
