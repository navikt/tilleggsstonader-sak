package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.DagligReiseBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.AvslagDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.OpphørDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.VedtakDagligReiseRequest
import org.springframework.stereotype.Service

@Service
class DagligReiseVedtakSteg(
    private val beregningService: DagligReiseBeregningService,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val opphørValideringService: OpphørValideringService,
    private val dagligReiseVedtakService: DagligReiseVedtakService,
) : BehandlingSteg<VedtakDagligReiseRequest> {
    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: VedtakDagligReiseRequest,
    ) {
        dagligReiseVedtakService.nullstillEksisterendeVedtakPåBehandling(saksbehandling)
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
            is AvslagDagligReiseDto -> dagligReiseVedtakService.lagreAvslag(saksbehandling, vedtak)
            is OpphørDagligReiseRequest -> beregnOgLagreOpphør(saksbehandling, vedtak)
        }
    }

    private fun lagreVedtaksperioderOgBeregn(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseDagligReiseRequest,
    ) {
        val vedtaksperioder = vedtak.vedtaksperioder()

        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(
                saksbehandling.id,
                vedtaksperioder,
            )

        val (beregningsresultat, rammevedtakPrivatBil) =
            beregningService.beregn(
                vedtaksperioder = vedtaksperioder,
                behandling = saksbehandling,
                typeVedtak = TypeVedtak.INNVILGELSE,
                tidligsteEndring = tidligsteEndring,
            )

        dagligReiseVedtakService.lagreInnvilgetVedtak(
            behandling = saksbehandling,
            beregningsresultat = beregningsresultat,
            rammevedtakPrivatBil = rammevedtakPrivatBil,
            vedtaksperioder = vedtaksperioder,
            begrunnelse = vedtak.begrunnelse,
            tidligsteEndring = tidligsteEndring,
        )
    }

    private fun beregnOgLagreOpphør(
        saksbehandling: Saksbehandling,
        vedtak: OpphørDagligReiseRequest,
    ) {
        feilHvis(saksbehandling.forrigeIverksatteBehandlingId == null) {
            "Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling"
        }
        feilHvis(vedtak.opphørsdato == null) {
            "Opphørsdato er ikke satt"
        }
        val opphørsdato = vedtak.opphørsdato
        val forrigeVedtak = dagligReiseVedtakService.hentVedtak(saksbehandling.forrigeIverksatteBehandlingId)

        opphørValideringService.validerVilkårperioder(saksbehandling, opphørsdato)

        opphørValideringService.validerVedtaksperioderAvkortetVedOpphør(
            forrigeBehandlingsVedtaksperioder = forrigeVedtak.data.vedtaksperioder,
            opphørsdato = opphørsdato,
        )

        val avkortetVedtaksperioder = dagligReiseVedtakService.avkortVedtaksperiodeVedOpphør(forrigeVedtak, opphørsdato)

        val (beregningsresultat, rammevedtakPrivatBil) =
            beregningService.beregn(
                vedtaksperioder = avkortetVedtaksperioder,
                behandling = saksbehandling,
                typeVedtak = TypeVedtak.OPPHØR,
                tidligsteEndring = opphørsdato,
            )
        opphørValideringService.validerIngenUtbetalingEtterOpphørsdatoDagligReise(
            beregningsresultatDagligReise = beregningsresultat,
            opphørsdato = opphørsdato,
        )

        dagligReiseVedtakService.lagreOpphørsvedtak(
            saksbehandling = saksbehandling,
            beregningsresultat = beregningsresultat,
            rammevedtakPrivatBil = rammevedtakPrivatBil,
            avkortetVedtaksperioder = avkortetVedtaksperioder,
            vedtak = vedtak,
        )

        tilkjentYtelseService.lagreTilkjentYtelse(
            behandlingId = saksbehandling.id,
            andeler =
                beregningsresultat.offentligTransport?.mapTilAndelTilkjentYtelse(saksbehandling)
                    ?: feil("Mangler beregningsresultat for offentlig transport"),
        )
    }

    private fun finnNesteSteg(vedtak: VedtakDagligReiseRequest): StegType =
        when (vedtak) {
            // TODO: Her kan man hoppe rett til beregning om man kun har registrert offentlig transport
            is InnvilgelseDagligReiseRequest -> StegType.KJØRELISTE
            is AvslagDagligReiseDto -> StegType.SEND_TIL_BESLUTTER
            is OpphørDagligReiseRequest -> StegType.SIMULERING
        }

    override fun stegType(): StegType = StegType.VEDTAK
}
