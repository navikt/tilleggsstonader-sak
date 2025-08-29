package no.nav.tilleggsstonader.sak.satsjustering

import no.nav.familie.prosessering.error.RekjørSenereException
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.OpprettRevurderingBehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.OpprettRevurdering
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.FerdigstillBehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerBeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.BeslutteVedtakSteg
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class SatsjusteringService(
    private val behandlingService: BehandlingService,
    private val revurderingBehandlingService: OpprettRevurderingBehandlingService,
    private val vedtakservice: VedtakService,
    private val beregnYtelseSteg: LæremidlerBeregnYtelseSteg,
    private val ferdigstillBehandlingSteg: FerdigstillBehandlingSteg,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val beslutteVedtakSteg: BeslutteVedtakSteg,
    private val stegService: StegService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val iverksettService: IverksettService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun kjørSatsjustering(behandlingId: BehandlingId) {
        val behandlingSomTrengerSatsjustering = behandlingService.hentBehandling(behandlingId)
        val fagsakId = behandlingSomTrengerSatsjustering.fagsakId
        if (behandlingService.finnesIkkeFerdigstiltBehandling(fagsakId)) {
            logger.info("Finnes en ikke ferdigstilt behandling for fagsakId=$fagsakId, kan ikke kjøre satsjustering.")
            /**
             * TODO
             * Hvis man kun skal ha en task per behandling, og at cron-job sjekker for at det ikke finnes en task for den behandlingId
             * så kan man kaste RekjørSenereException her, og tasken er ansvarlig for å kjøre på nytt dagen etter.
             */
            throw RekjørSenereException(
                "Fagsak=$fagsakId har en ikke ferdigstilt behandling, kan ikke kjøre satsjustering.",
                LocalDate.now().plusDays(1).atTime(7, 0),
            )
        }
        val sisteIverksatteBehandling = behandlingService.finnSisteIverksatteBehandling(fagsakId)
        if (sisteIverksatteBehandling?.id != behandlingId) {
            logger.info(
                "Siste iverksatte behandling=${sisteIverksatteBehandling?.id} er ikke lik behandlingId=$behandlingId, kan ikke kjøre satsjustering.",
            )
            return
        }

        opprettRevurderingOgKjørSatsendring(fagsakId)
    }

    private fun opprettRevurderingOgKjørSatsendring(fagsakId: FagsakId) {
        val revurderingId =
            revurderingBehandlingService.opprettBehandling(
                OpprettRevurdering(
                    fagsakId = fagsakId,
                    årsak = BehandlingÅrsak.SATSENDRING,
                    valgteBarn = emptySet(),
                    nyeOpplysningerMetadata = null,
                    kravMottatt = null,
                    skalOppretteOppgave = false,
                ),
            )
        val revurdering = behandlingService.hentSaksbehandling(revurderingId)

        val forrigeIverksatteBehandlingId =
            revurdering.forrigeIverksatteBehandlingId
                ?: error("Revurdering for fagsak=${revurdering.fagsakId} har ikke fått forrigeIverksattBehandlingId")
        val vedtaksperioder =
            vedtakservice.hentVedtaksperioder(behandlingId = forrigeIverksatteBehandlingId).tilDto()

        beregnYtelseSteg.lagreVedtakForSatsjustering(
            saksbehandling = revurdering,
            vedtak = InnvilgelseLæremidlerRequest(vedtaksperioder = vedtaksperioder),
            satsjusteringFra = finnDatoForSatsjustering(revurdering),
        )

        // TODO finne ut hva vi gjør med totrinnskontroll ved satsjustering
        behandlingService.oppdaterStatusPåBehandling(revurdering.id, BehandlingStatus.FATTER_VEDTAK)
        behandlingService.oppdaterResultatPåBehandling(revurdering.id, BehandlingResultat.INNVILGET)
        iverksettService.iverksettBehandlingFørsteGang(revurdering.id)

        // totrinnskontroll
        // iverksett
        // behandlingService.oppdaterStegPåBehandling(revurdering.id, StegType.BESLUTTE_VEDTAK)

        // TODO slett ferdigstillBehandlingSteg - den burde bli håndtert automatisk etter at behandlingen er iverksatt
        ferdigstillBehandlingSteg.utførSteg(revurdering, null)
    }

    private fun finnDatoForSatsjustering(revurdering: Saksbehandling): LocalDate =
        tilkjentYtelseService
            .hentForBehandling(revurdering.forrigeIverksatteBehandlingId!!)
            .andelerTilkjentYtelse
            .filter { it.statusIverksetting == StatusIverksetting.VENTER_PÅ_SATS_ENDRING }
            .minOf { it.fom }
}
