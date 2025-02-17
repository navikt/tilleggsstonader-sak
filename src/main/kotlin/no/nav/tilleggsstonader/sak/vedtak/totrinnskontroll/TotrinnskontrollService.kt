package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext.NAVIDENT_REGEX
import no.nav.tilleggsstonader.sak.statistikk.task.BehandlingsstatistikkTask
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollRepository
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Årsaker
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.SendTilBeslutterRequest
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.StatusTotrinnskontrollDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.TotrinnkontrollStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.TotrinnskontrollDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TotrinnskontrollService(
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val behandlingService: BehandlingService,
    private val tilgangService: TilgangService,
    private val taskService: TaskService,
    private val totrinnskontrollRepository: TotrinnskontrollRepository,
) {
    @Transactional
    fun sendtilBeslutter(
        saksbehandling: Saksbehandling,
        sendTilBeslutterRequest: SendTilBeslutterRequest,
    ) {
        val eksisterandeTotrinnskontroll =
            totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(saksbehandling.id)

        if (eksisterandeTotrinnskontroll != null) {
            feilHvis(
                (
                    eksisterandeTotrinnskontroll.status != TotrinnInternStatus.ANGRET &&
                        eksisterandeTotrinnskontroll.status != TotrinnInternStatus.UNDERKJENT
                ),
            ) {
                "Kan ikke sende til beslutter da det eksisterer en totrinnskontroll med status=${eksisterandeTotrinnskontroll.status}"
            }
        } else {
            feilHvis(sendTilBeslutterRequest.kommentarTilBeslutter != null) {
                "Kan ikke legge ved kommentar til beslutter dersom behandlingen ikke er tidligere underkjent"
            }
        }

        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingId = saksbehandling.id,
            stegtype = saksbehandling.steg,
            utfall = null,
            metadata = mapOf("kommentarTilBeslutter" to sendTilBeslutterRequest.kommentarTilBeslutter),
        )

        totrinnskontrollRepository.insert(
            Totrinnskontroll(
                behandlingId = saksbehandling.id,
                saksbehandler = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
                status = TotrinnInternStatus.KAN_FATTE_VEDTAK,
                begrunnelse = sendTilBeslutterRequest.kommentarTilBeslutter,
            ),
        )

        taskService.save(BehandlingsstatistikkTask.opprettVedtattTask(behandlingId = saksbehandling.id))
    }

    @Transactional
    fun angreSendTilBeslutter(behandlingId: BehandlingId) {
        val eksisterandeTotrinnskontroll =
            totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandlingId)

        feilHvis((eksisterandeTotrinnskontroll == null || eksisterandeTotrinnskontroll.status != TotrinnInternStatus.KAN_FATTE_VEDTAK)) {
            "Kan ikke angre når status=${eksisterandeTotrinnskontroll?.status}"
        }
        oppdaterStatusPåTotrinnskontroll(TotrinnInternStatus.ANGRET, eksisterandeTotrinnskontroll)

        // TODO: Vurder om denne endringen skal trigge behandlingsstatistikk til DVH (med status PÅBEGYNT)
    }

    /**
     * Lagrer data om besluttning av totrinnskontroll
     * og returnerer navIdent til saksbehandleren som sendte behandling til beslutter
     */
    @Transactional
    fun lagreTotrinnskontrollOgReturnerSaksbehandler(
        saksbehandling: Saksbehandling,
        beslutteVedtak: BeslutteVedtakDto,
    ): String {
        settBeslutter(saksbehandling.id)
        val sisteTotrinnskontroll =
            totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandlingId = saksbehandling.id)
                ?: error("Finnes ikke eksisterende Tostrinnskontroll på behandling")
        if (sisteTotrinnskontroll.status != TotrinnInternStatus.KAN_FATTE_VEDTAK) {
            throw Feil(
                message = "Status for totrinnskontoll er ikke korrekt, status =  ${sisteTotrinnskontroll.status} ",
                frontendFeilmelding = "Status for totrinnskontoll er ikke korrekt, vennligst last side på nytt ",
            )
        }

        if (beslutterErLikBehandler(sisteTotrinnskontroll)) {
            throw Feil(
                message = "Beslutter er samme som saksbehandler, kan ikke utføre totrinnskontroll",
                frontendFeilmelding = "Beslutter er samme som behandler, samme person kan ikke godkjenne vedtaket",
            )
        }
        val nyStatus = if (beslutteVedtak.godkjent) BehandlingStatus.IVERKSETTER_VEDTAK else BehandlingStatus.UTREDES
        val nyTotrinnsKontrollStatus =
            if (beslutteVedtak.godkjent) TotrinnInternStatus.GODKJENT else TotrinnInternStatus.UNDERKJENT
        val utfall =
            if (beslutteVedtak.godkjent) StegUtfall.BESLUTTE_VEDTAK_GODKJENT else StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT

        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingId = saksbehandling.id,
            stegtype = saksbehandling.steg,
            utfall = utfall,
            metadata = beslutteVedtak,
        )

        behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, nyStatus)
        if (nyTotrinnsKontrollStatus == TotrinnInternStatus.UNDERKJENT) {
            oppdaterUtfallogÅrsakPåTotrinnskontroll(beslutteVedtak, sisteTotrinnskontroll, nyTotrinnsKontrollStatus)
            // TODO: Vurder om vi skal sende behandlingsstatistikk her
        } else {
            oppdaterStatusPåTotrinnskontroll(nyTotrinnsKontrollStatus, sisteTotrinnskontroll)
            taskService.save(BehandlingsstatistikkTask.opprettBesluttetTask(behandlingId = saksbehandling.id))
        }

        return sisteTotrinnskontroll.saksbehandler
    }

    fun hentSaksbehandlerSomSendteTilBeslutter(behandlingId: BehandlingId): String {
        val totrinnskontrollSaksbehandler =
            totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandlingId)
                ?: error("Finner ikke totrinnskontroll for behandling=$behandlingId")
        return totrinnskontrollSaksbehandler.saksbehandler
    }

    fun hentBeslutter(behandlingId: BehandlingId): String? =
        totrinnskontrollRepository
            .findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandlingId)
            ?.beslutter
            ?.takeIf { NAVIDENT_REGEX.matches(it) }

    fun hentTotrinnskontroll(behandlingId: BehandlingId): Totrinnskontroll? =
        totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandlingId)

    fun hentTotrinnskontrollStatus(behandlingId: BehandlingId): StatusTotrinnskontrollDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val behandlingStatus = behandling.status

        // Behandling ligger som et ytre skall for Totrinnskontroll og håndterer statuser før og etter totrinnskontrolls opprettelse og ferdigstillelse
        if (behandlingErGodkjentEllerOpprettet(behandlingStatus)) {
            return StatusTotrinnskontrollDto(TotrinnkontrollStatus.UAKTUELT)
        }

        return when (behandlingStatus) {
            BehandlingStatus.FATTER_VEDTAK -> finnStatusForVedtakSomSkalFattes(behandling)
            BehandlingStatus.UTREDES -> finnStatusForVedtakSomErFattet(behandlingId)
            else -> error("Har ikke lagt til håndtering av behandlingStatus=$behandlingStatus")
        }
    }

    private fun oppdaterUtfallogÅrsakPåTotrinnskontroll(
        beslutteVedtak: BeslutteVedtakDto,
        sisteTotrinnskontroll: Totrinnskontroll,
        nyTotrinnsKontrollStatus: TotrinnInternStatus,
    ): Totrinnskontroll {
        val begrunnelse = beslutteVedtak.begrunnelse
        val årsakUnderkjent = beslutteVedtak.årsakerUnderkjent

        return totrinnskontrollRepository.update(
            sisteTotrinnskontroll.copy(
                begrunnelse = begrunnelse,
                årsakerUnderkjent = Årsaker(årsakUnderkjent),
                status = nyTotrinnsKontrollStatus,
            ),
        )
    }

    private fun behandlingErGodkjentEllerOpprettet(behandlingStatus: BehandlingStatus) =
        behandlingStatus == BehandlingStatus.FERDIGSTILT ||
            behandlingStatus == BehandlingStatus.IVERKSETTER_VEDTAK ||
            behandlingStatus == BehandlingStatus.SATT_PÅ_VENT ||
            behandlingStatus == BehandlingStatus.OPPRETTET

    private fun finnStatusForVedtakSomSkalFattes(behandling: Behandling): StatusTotrinnskontrollDto {
        val behandlingId = behandling.id

        if (behandling.steg != StegType.BESLUTTE_VEDTAK) {
            throw Feil(
                message = "Totrinnskontroll kan ikke gjennomføres da steg på behandling er feil , steg = ${behandling.steg}",
                frontendFeilmelding = "Feil i steg, kontakt brukerstøtte id=$behandlingId",
            )
        }
        val totrinnskontroll =
            totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandlingId)
                ?: error("mangler totrinnskontroll på behandling id=$behandlingId")

        val totrinnskontrollDto =
            TotrinnskontrollDto(
                opprettetAv = totrinnskontroll.saksbehandler,
                opprettetTid = totrinnskontroll.sporbar.opprettetTid,
                begrunnelse = totrinnskontroll.begrunnelse,
            )

        return if (beslutterErLikBehandler(totrinnskontroll) || !tilgangService.harTilgangTilRolle(BehandlerRolle.BESLUTTER)
        ) {
            StatusTotrinnskontrollDto(
                TotrinnkontrollStatus.IKKE_AUTORISERT,
                totrinnskontrollDto,
            )
        } else {
            StatusTotrinnskontrollDto(
                TotrinnkontrollStatus.KAN_FATTE_VEDTAK,
                totrinnskontrollDto,
            )
        }
    }

    /**
     * Hvis behandlingen utredes sjekkes det for om det finnes ett tidligere beslutt, som då kun kan være underkjent
     */
    private fun finnStatusForVedtakSomErFattet(behandlingId: BehandlingId): StatusTotrinnskontrollDto {
        val totrinnskontroll =
            totrinnskontrollRepository.findTopByBehandlingIdAndStatusNotOrderBySporbarEndretEndretTidDesc(
                behandlingId,
                TotrinnInternStatus.ANGRET,
            ) ?: return StatusTotrinnskontrollDto(TotrinnkontrollStatus.UAKTUELT)
        return when (totrinnskontroll.status) {
            TotrinnInternStatus.UNDERKJENT -> {
                val beslutter = totrinnskontroll.beslutter
                val årsakerUnderkjent = totrinnskontroll.årsakerUnderkjent?.årsaker
                if (beslutter == null || årsakerUnderkjent == null) {
                    error("Mangler beslutter/årsaker på totrinnskontroll=${totrinnskontroll.id}")
                }
                StatusTotrinnskontrollDto(
                    TotrinnkontrollStatus.TOTRINNSKONTROLL_UNDERKJENT,
                    TotrinnskontrollDto(
                        opprettetAv = beslutter,
                        opprettetTid = totrinnskontroll.sporbar.endret.endretTid,
                        godkjent = false,
                        begrunnelse = totrinnskontroll.begrunnelse,
                        årsakerUnderkjent = årsakerUnderkjent,
                    ),
                )
            }

            else ->
                error(
                    "Skal ikke kunne være annen status enn UNDERKJENT når " +
                        "behandligStatus!=${BehandlingStatus.FATTER_VEDTAK}",
                )
        }
    }

    private fun beslutterErLikBehandler(beslutteTotrinnskontroll: Totrinnskontroll): Boolean =
        SikkerhetContext.hentSaksbehandler() == beslutteTotrinnskontroll.saksbehandler

    private fun oppdaterStatusPåTotrinnskontroll(
        status: TotrinnInternStatus,
        gjeldeneTotrinnskontroll: Totrinnskontroll,
    ): Totrinnskontroll {
        // generisk metode for å logge endringene som er utført
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} " +
                "endrer på totrinnskontroll knyttet til behandlingId $gjeldeneTotrinnskontroll.id" +
                "til $status",
        )
        return totrinnskontrollRepository.update(gjeldeneTotrinnskontroll.copy(status = status))
    }

    private fun settBeslutter(behandlingId: BehandlingId) {
        val totrinnskontroll =
            totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandlingId)
        if (totrinnskontroll != null) {
            totrinnskontrollRepository.update(totrinnskontroll.copy(beslutter = SikkerhetContext.hentSaksbehandlerEllerSystembruker()))
        }
    }
}
