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
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext.NAVIDENT_REGEX
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnsKontrollStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollRepository
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.StatusTotrinnskontrollDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.TotrinnkontrollStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.TotrinnskontrollDto
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

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
    fun opprettTotrinnskontroll(saksbehandling: Saksbehandling, beslutteVedtak: BeslutteVedtakDto): Totrinnskontroll {
        val nytotrinnskontroll = Totrinnskontroll(
            behandlingId = saksbehandling.id,
            sporbar = Sporbar(),
            saksbehandler = Sporbar().opprettetAv,
            status = TotrinnsKontrollStatus.SKAL_TOTRINNSKONTROLLERES,
        )
        return totrinnskontrollRepository.insert(nytotrinnskontroll)
    }

    @Transactional
    fun lagreTotrinnskontrollOgReturnerSaksbehandler(
        saksbehandling: Saksbehandling,
        beslutteVedtak: BeslutteVedtakDto,

    ): String {
        val sisteTotrinnskontroll =
            totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandlingId = saksbehandling.id)
                ?: error("Finnes ikke eksisterende Tostrinnskontroll på behandling")
        if (sisteTotrinnskontroll.status != TotrinnsKontrollStatus.KLAR) {
            throw Feil(
                message = "Siste innslag i behandlingshistorikken har feil status=${sisteTotrinnskontroll.status}",
                frontendFeilmelding = "Status for totrinnskontroll er feil, last siden på nytt",
            )
        }

        beslutterErLikBehandler(sisteTotrinnskontroll) {
            throw ApiFeil(
                "Beslutter kan ikke behandle en behandling som den selv har sendt til beslutter",
                HttpStatus.BAD_REQUEST,
            )
        }
        // refaktorere til at totrinns er frikobla fra behandlinga
        val nyStatus = if (beslutteVedtak.godkjent) BehandlingStatus.IVERKSETTER_VEDTAK else BehandlingStatus.UTREDES
        val nyTotrinnsKontrollStatus = if (beslutteVedtak.godkjent) TotrinnsKontrollStatus.KLAR else TotrinnsKontrollStatus.UNDERKJENT // rett logikk??
        val utfall = if (beslutteVedtak.godkjent) StegUtfall.BESLUTTE_VEDTAK_GODKJENT else StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT

        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingId = saksbehandling.id,
            stegtype = saksbehandling.steg,
            utfall = utfall,
            metadata = beslutteVedtak,
        )

        behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, nyStatus)
        oppdaterStatusPåTotrinnskontroll(sisteTotrinnskontroll.id, nyTotrinnsKontrollStatus)
        return sisteTotrinnskontroll.saksbehandler
    }

    fun hentSaksbehandlerSomSendteTilBeslutter(behandlingId: UUID): String {
        val totrinnskontrollSaksbehandler = totrinnskontrollRepository.findTopByBehandlingIdAndStatusOrderBySporbarEndretEndretTidDesc(behandlingId, TotrinnsKontrollStatus.KLAR)

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
        return if (beslutterErLikBehandler(totrinnskontroll) {
                throw ApiFeil(
                    "Beslutter kan ikke behandle en behandling som den selv har sendt til beslutter",
                    HttpStatus.BAD_REQUEST,
                )
            } || !tilgangService.harTilgangTilRolle(BehandlerRolle.BESLUTTER)
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
            TotrinnsKontrollStatus.UNDERKJENT -> {
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
            TotrinnsKontrollStatus.ANGRE_SEND_TIL_BESLUTTER -> StatusTotrinnskontrollDto(TotrinnkontrollStatus.UAKTUELT)
            else -> error(
                "Skal ikke kunne være annen status enn UNDERKJENT når " +
                    "behandligStatus!=${BehandlingStatus.FATTER_VEDTAK}",
            )
        }
    }

    private fun beslutterErLikBehandler(beslutteTotrinnskontroll: Totrinnskontroll, function: () -> Nothing) =
        SikkerhetContext.hentSaksbehandlerEllerSystembruker() == beslutteTotrinnskontroll.saksbehandler

    private fun oppdaterStatusPåTotrinnskontroll(toTrinnsId: UUID, status: TotrinnsKontrollStatus): Totrinnskontroll {
        val gjeldeneTotrinnskontroll = totrinnskontrollRepository.findByIdOrThrow(toTrinnsId)
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} endrer status på behandling $toTrinnsId " +
                "fra ${gjeldeneTotrinnskontroll.status} til $status",
        )
        return totrinnskontrollRepository.update(gjeldeneTotrinnskontroll.copy(status = status))
    }
}
