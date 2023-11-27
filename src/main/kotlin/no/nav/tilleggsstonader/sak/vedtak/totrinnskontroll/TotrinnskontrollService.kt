package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll

import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext.NAVIDENT_REGEX
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollRepository
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Årsaker
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.StatusTotrinnskontrollDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.TotrinnkontrollStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.TotrinnskontrollDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class TotrinnskontrollService(
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val behandlingService: BehandlingService,
    private val tilgangService: TilgangService,
    private val totrinnskontrollRepository: TotrinnskontrollRepository,
) {

    /**
     * Lagrer data om besluttning av totrinnskontroll
     * og returnerer navIdent til saksbehandleren som sendte behandling til beslutter
     */
    @Transactional
    fun lagreTotrinnskontrollOgReturnerSaksbehandler(
        saksbehandling: Saksbehandling,
        beslutteVedtak: BeslutteVedtakDto,

    ): String {
        val sisteTotrinnskontroll =
            totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandlingId = saksbehandling.id)
                ?: totrinnskontrollRepository.insert(Totrinnskontroll(
                    behandlingId =  saksbehandling.id,
                    sporbar = Sporbar(opprettetAv =  SikkerhetContext.hentSaksbehandlerEllerSystembruker(), opprettetTid = LocalDateTime.now()),
                    status = TotrinnInternStatus.KAN_FATTE_VEDTAK,
                    saksbehandler =  SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
                ))//lagre ned nytt totrinnsobjekt til db.
        if (sisteTotrinnskontroll.status != TotrinnInternStatus.KAN_FATTE_VEDTAK) {
            throw Feil(
                message = "Siste innslag i behandlingshistorikken har feil status=${sisteTotrinnskontroll.status}",
                frontendFeilmelding = "Status for totrinnskontroll er feil, last siden på nytt",
            )
        }
        // gjer om bør ikkje kaste feil ved at saksbehandler og beslutter er samme, dette må håndteres bedre ovanfor bruker.
        if (beslutterErLikBehandler(sisteTotrinnskontroll)) {
            throw Feil(
                message = "Beslutter som er tilordnet kontrollen er samme som er saksbehandler ${sisteTotrinnskontroll.status}",
                frontendFeilmelding = "Kan ikke settes da saksbehandler og beslutter er samme",
            )
        }
        // refaktorere til at totrinns er frikobla fra behandlinga
        val nyStatus = if (beslutteVedtak.godkjent) BehandlingStatus.IVERKSETTER_VEDTAK else BehandlingStatus.UTREDES
        val nyTotrinnsKontrollStatus = if (beslutteVedtak.godkjent) TotrinnInternStatus.GODKJENT else TotrinnInternStatus.UNDERKJENT
        val utfall = if (beslutteVedtak.godkjent) StegUtfall.BESLUTTE_VEDTAK_GODKJENT else StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT

        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingId = saksbehandling.id,
            stegtype = saksbehandling.steg,
            utfall = utfall,
            metadata = beslutteVedtak,
        )

        behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, nyStatus)
        if (nyTotrinnsKontrollStatus == TotrinnInternStatus.UNDERKJENT) { oppdaterUtfallogÅrsakPåTotrinnskontroll(beslutteVedtak, sisteTotrinnskontroll) } else oppdaterStatusPåTotrinnskontroll(nyTotrinnsKontrollStatus, sisteTotrinnskontroll)
        return sisteTotrinnskontroll.saksbehandler
    }

    fun settBeslutterForTotrinnsKontroll(id: UUID, beslutter: String): Totrinnskontroll {
        val gjeldeneTotrinnskontroll = totrinnskontrollRepository.findByIdOrThrow(id)
        return totrinnskontrollRepository.update(gjeldeneTotrinnskontroll.copy(beslutter = beslutter))
    }

    fun hentSaksbehandlerSomSendteTilBeslutter(behandlingId: UUID): String {
        val totrinnskontrollSaksbehandler = totrinnskontrollRepository.findTopByBehandlingIdAndStatusOrderBySporbarEndretEndretTidDesc(behandlingId, TotrinnInternStatus.GODKJENT)
        // gjøre om på denne? vil no returerne tom som ok?
        return totrinnskontrollSaksbehandler.saksbehandler ?: throw Feil("Fant ikke saksbehandler som sendte til beslutter")
    }

    fun hentBeslutter(behandlingId: UUID): String? {
        return totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandlingId)
            ?.beslutter
            ?.takeIf { NAVIDENT_REGEX.matches(it) }
    }

    fun hentTotrinnskontrollStatus(behandlingId: UUID): StatusTotrinnskontrollDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val behandlingStatus = behandling.status

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
    ): Totrinnskontroll {
        val begrunnelse = beslutteVedtak.begrunnelse
        val årsakUnderkjent = beslutteVedtak.årsakerUnderkjent as Årsaker
        return totrinnskontrollRepository.update(sisteTotrinnskontroll.copy(begrunnelse = begrunnelse, årsakerUnderkjent = årsakUnderkjent))
    }

    private fun behandlingErGodkjentEllerOpprettet(behandlingStatus: BehandlingStatus) =
        behandlingStatus == BehandlingStatus.FERDIGSTILT ||
            behandlingStatus == BehandlingStatus.IVERKSETTER_VEDTAK ||
            behandlingStatus == BehandlingStatus.SATT_PÅ_VENT ||
            behandlingStatus == BehandlingStatus.OPPRETTET

    /**
     * Hvis behandlingsstatus er FATTER_VEDTAK så sjekkes det att saksbehandleren er autorisert til å fatte vedtak
     */
    private fun finnStatusForVedtakSomSkalFattes(behandling: Behandling): StatusTotrinnskontrollDto {
        val behandlingId = behandling.id

        if (behandling.steg != StegType.SEND_TIL_BESLUTTER) {
            throw Feil(
                message = "Totrinnskontroll kan ikke gjennomføres da steg på behandling er feil , steg = ${behandling.steg}",
                frontendFeilmelding = "Feil i steg, kontakt brukerstøtte id=$behandlingId",
            )
        }
        val totrinnskontroll = totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandlingId)
            ?: error("mangler totrinnskontroll på behandling id=$behandlingId")
        return if (beslutterErLikBehandler(totrinnskontroll) || !tilgangService.harTilgangTilRolle(BehandlerRolle.BESLUTTER)
        ) {
            StatusTotrinnskontrollDto(
                TotrinnkontrollStatus.IKKE_AUTORISERT,
                TotrinnskontrollDto(totrinnskontroll.saksbehandler, totrinnskontroll.sporbar.opprettetTid),
            )
        } else {
            StatusTotrinnskontrollDto(TotrinnkontrollStatus.KAN_FATTE_VEDTAK)
        }
    }

    /**
     * Hvis behandlingen utredes sjekkes det for om det finnes ett tidligere beslutt, som då kun kan være underkjent
     */
    private fun finnStatusForVedtakSomErFattet(behandlingId: UUID): StatusTotrinnskontrollDto {
        val totrinnskontroll = totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandlingId)
            ?: return StatusTotrinnskontrollDto(TotrinnkontrollStatus.UAKTUELT)
        return when (totrinnskontroll.status) {
            TotrinnInternStatus.UNDERKJENT -> { // TODO transformer
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
            TotrinnInternStatus.ANGRET -> StatusTotrinnskontrollDto(TotrinnkontrollStatus.UAKTUELT)
            else -> error(
                "Skal ikke kunne være annen status enn UNDERKJENT når " +
                    "behandligStatus!=${BehandlingStatus.FATTER_VEDTAK}",
            )
        }
    }

    private fun beslutterErLikBehandler(beslutteTotrinnskontroll: Totrinnskontroll) =
        SikkerhetContext.hentSaksbehandlerEllerSystembruker() == beslutteTotrinnskontroll.saksbehandler

    private fun oppdaterStatusPåTotrinnskontroll(status: TotrinnInternStatus, gjeldeneTotrinnskontroll: Totrinnskontroll): Totrinnskontroll {
        // generisk metode for å logge endringene som er utført
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} endrer på totrinnskontroll knyttet til behandlingId $gjeldeneTotrinnskontroll.id" +
                "til $status",
        )
        return totrinnskontrollRepository.update(gjeldeneTotrinnskontroll.copy(status = status))
    }
}
