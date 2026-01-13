package no.nav.tilleggsstonader.sak.satsjustering

import no.nav.familie.prosessering.error.RekjørSenereException
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.OpprettRevurderingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.OpprettRevurdering
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.vent.SettBehandlingPåVent
import no.nav.tilleggsstonader.sak.behandling.vent.SettBehandlingPåVentOppgaveMetadata
import no.nav.tilleggsstonader.sak.behandling.vent.SettPåVentService
import no.nav.tilleggsstonader.sak.behandling.vent.TaAvVentDto
import no.nav.tilleggsstonader.sak.behandling.vent.TaAvVentService
import no.nav.tilleggsstonader.sak.behandling.vent.ÅrsakSettPåVent
import no.nav.tilleggsstonader.sak.behandlingsflyt.FerdigstillBehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterBeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.InnvilgelseBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerBeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class UtførSatsjusteringService(
    private val behandlingService: BehandlingService,
    private val revurderingBehandlingService: OpprettRevurderingService,
    private val vedtakservice: VedtakService,
    private val beregnYtelseStegLæremidler: LæremidlerBeregnYtelseSteg,
    private val beregnYtelseStegBoutgifter: BoutgifterBeregnYtelseSteg,
    private val ferdigstillBehandlingSteg: FerdigstillBehandlingSteg,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val iverksettService: IverksettService,
    private val faktaGrunnlagService: FaktaGrunnlagService,
    private val settPåVentService: SettPåVentService,
    private val taAvVentService: TaAvVentService,
    private val oppgaveService: OppgaveService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun kjørSatsjustering(behandlingId: BehandlingId) {
        val behandlingSomTrengerSatsjustering = behandlingService.hentBehandling(behandlingId)
        val fagsakId = behandlingSomTrengerSatsjustering.fagsakId

        if (behandlingService.finnesIkkeFerdigstiltBehandling(fagsakId)) {
            val åpneBehandlinger = hentIkkeFerdigstilteBehandlinger(fagsakId)

            val kanSettesPåVentForSatsjustering = kanSettesPåVentForSatsjustering(åpneBehandlinger)
            if (kanSettesPåVentForSatsjustering != null) {
                kjørSatsjusteringMedBehandlingPåVent(kanSettesPåVentForSatsjustering, fagsakId)
            } else {
                logger.info("Finnes ikke-ferdigstilte behandlinger for fagsakId=$fagsakId som ikke kan settes på vent, rekjører senere.")
                throw RekjørSenereException(
                    "Fagsak=$fagsakId har en ikke ferdigstilt behandling, kan ikke kjøre satsjustering.",
                    LocalDate.now().plusDays(1).atTime(7, 0),
                )
            }
            return
        }

        val sisteIverksatteBehandling = behandlingService.finnSisteIverksatteBehandling(fagsakId)
        if (sisteIverksatteBehandling?.id != behandlingSomTrengerSatsjustering.id) {
            logger.info(
                "Siste iverksatte behandling=${sisteIverksatteBehandling?.id} er ikke lik behandlingId=${behandlingSomTrengerSatsjustering.id}, kan ikke kjøre satsjustering.",
            )
            return
        }

        opprettRevurderingOgKjørSatsendring(fagsakId)
    }

    private fun hentIkkeFerdigstilteBehandlinger(fagsakId: FagsakId): List<Behandling> =
        behandlingService
            .hentBehandlinger(fagsakId)
            .filter { it.status != BehandlingStatus.FERDIGSTILT }

    /**
     * Sjekker om det kun finnes én åpen behandling som er i status OPPRETTET med ufordelt oppgave.
     * I så fall kan vi sette den på vent under satsjustering.
     */
    private fun kanSettesPåVentForSatsjustering(åpneBehandlinger: List<Behandling>): Behandling? {
        if (åpneBehandlinger.size != 1) {
            return null
        }

        val behandling = åpneBehandlinger.single()
        if (behandling.status != BehandlingStatus.OPPRETTET) {
            return null
        }

        val oppgave = oppgaveService.hentÅpenBehandlingsoppgave(behandling.id)
        if (oppgave?.tilordnetSaksbehandler != null) {
            return null
        }

        return behandling
    }

    private fun kjørSatsjusteringMedBehandlingPåVent(
        behandlingSomSettesPåVent: Behandling,
        fagsakId: FagsakId,
    ) {
        val oppgave = oppgaveService.hentAktivBehandleSakOppgave(behandlingSomSettesPåVent.id)
        val frist = oppgave.fristFerdigstillelse ?: error("Oppgave mangler frist for behandling=${behandlingSomSettesPåVent.id}")

        settPåVentService.settPåVent(
            behandlingId = behandlingSomSettesPåVent.id,
            request =
                SettBehandlingPåVent(
                    årsaker = listOf(ÅrsakSettPåVent.FOR_SATSJUSTERING),
                    frist = frist,
                    kommentar = null,
                    oppgaveMetadata = SettBehandlingPåVentOppgaveMetadata.IkkeOppdaterOppgave,
                ),
        )

        opprettRevurderingOgKjørSatsendring(fagsakId)
        taAvVentService.taAvVent(behandlingSomSettesPåVent.id, TaAvVentDto(settBehandlingStatusTil = BehandlingStatus.OPPRETTET))
    }

    private fun opprettRevurderingOgKjørSatsendring(fagsakId: FagsakId) {
        val revurdering = opprettRevurderingForSatsendring(fagsakId)

        val forrigeIverksatteBehandlingId =
            revurdering.forrigeIverksatteBehandlingId
                ?: error("Revurdering for fagsak=${revurdering.fagsakId} har ikke fått forrigeIverksattBehandlingId")
        val vedtaksperioder =
            vedtakservice.hentVedtaksperioder(behandlingId = forrigeIverksatteBehandlingId).tilDto()

        when (revurdering.stønadstype) {
            Stønadstype.LÆREMIDLER ->
                beregnYtelseStegLæremidler.lagreVedtakForSatsjustering(
                    saksbehandling = revurdering,
                    vedtak = InnvilgelseLæremidlerRequest(vedtaksperioder = vedtaksperioder),
                    satsjusteringFra = finnDatoForSatsjustering(revurdering),
                )

            Stønadstype.BOUTGIFTER ->
                beregnYtelseStegBoutgifter.lagreVedtakForSatsjustering(
                    saksbehandling = revurdering,
                    vedtak = InnvilgelseBoutgifterRequest(vedtaksperioder = vedtaksperioder),
                    satsjusteringFra = finnDatoForSatsjustering(revurdering),
                )

            else -> error("Stønadstype ${revurdering.stønadstype} støttes ikke for satsjustering.")
        }

        // Kaller disse direkte for å hoppe over totrinnskontroll
        behandlingService.oppdaterStatusPåBehandling(revurdering.id, BehandlingStatus.FATTER_VEDTAK)
        behandlingService.oppdaterResultatPåBehandling(revurdering.id, BehandlingResultat.INNVILGET)
        behandlingService.oppdaterStegPåBehandling(revurdering.id, StegType.FERDIGSTILLE_BEHANDLING)
        iverksettService.iverksettBehandlingFørsteGang(revurdering.id)

        // Her lages også internt vedtak og behandling- og vedtaksstatistikk
        ferdigstillBehandlingSteg.utførSteg(revurdering, null)
    }

    private fun opprettRevurderingForSatsendring(fagsakId: FagsakId): Saksbehandling {
        val revurderingId =
            revurderingBehandlingService.opprettRevurdering(
                OpprettRevurdering(
                    fagsakId = fagsakId,
                    årsak = BehandlingÅrsak.SATSENDRING,
                    valgteBarn = emptySet(), // TODO - kopier over barn
                    nyeOpplysningerMetadata = null,
                    kravMottatt = null,
                    skalOppretteOppgave = false,
                ),
            )
        // Gjenbruker grunnlag fra forrige behandling ved satsjustering
        faktaGrunnlagService.kopierGrunnlagsdataFraForrigeIverksatteBehandling(revurderingId)
        return behandlingService.hentSaksbehandling(revurderingId)
    }

    private fun finnDatoForSatsjustering(revurdering: Saksbehandling): LocalDate =
        tilkjentYtelseService
            .hentForBehandling(revurdering.forrigeIverksatteBehandlingId!!)
            .andelerTilkjentYtelse
            .filter { it.statusIverksetting == StatusIverksetting.VENTER_PÅ_SATS_ENDRING }
            .minOf { it.fom }
}
