package no.nav.tilleggsstonader.sak.vedtak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext.NAVIDENT_REGEX
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vedtak.dto.StatusTotrinnskontrollDto
import no.nav.tilleggsstonader.sak.vedtak.dto.TotrinnkontrollStatus
import no.nav.tilleggsstonader.sak.vedtak.dto.TotrinnskontrollDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class TotrinnskontrollService(
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val behandlingService: BehandlingService,
    private val tilgangService: TilgangService,
) {

    /**
     * Lagrer data om besluttning av totrinnskontroll
     * og returnerer navIdent til saksbehandleren som sendte behandling til beslutter
     */
    @Transactional
    fun lagreTotrinnskontrollOgReturnerBehandler(
        saksbehandling: Saksbehandling,
        beslutteVedtak: BeslutteVedtakDto,
        //  vedtakErUtenBeslutter: VedtakErUtenBeslutter,
    ): String {
        val sisteBehandlingshistorikk =
            behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId = saksbehandling.id)
        if (sisteBehandlingshistorikk.steg != StegType.SEND_TIL_BESLUTTER) {
            throw Feil(
                message = "Siste innslag i behandlingshistorikken har feil steg=${sisteBehandlingshistorikk.steg}",
                frontendFeilmelding = "Behandlingen er i feil steg, last siden på nytt",
            )
        }
/** Lar stå utkommentert fram til koden leser Totrinnsdataobjektetet der dette ligger tilgjengeleg **/
        /**  if (!vedtakErUtenBeslutter.value && beslutterErLikBehandler(sisteBehandlingshistorikk)) {
         throw ApiFeil(
         "Beslutter kan ikke behandle en behandling som den selv har sendt til beslutter",
         HttpStatus.BAD_REQUEST,
         )
         } **/

        val nyStatus = if (beslutteVedtak.godkjent) BehandlingStatus.IVERKSETTER_VEDTAK else BehandlingStatus.UTREDES
        val utfall = if (beslutteVedtak.godkjent) StegUtfall.BESLUTTE_VEDTAK_GODKJENT else StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT

        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingId = saksbehandling.id,
            stegtype = saksbehandling.steg,
            utfall = utfall,
            metadata = beslutteVedtak,
        )

        behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, nyStatus)
        return sisteBehandlingshistorikk.opprettetAv
    }

    fun hentSaksbehandlerSomSendteTilBeslutter(behandlingId: UUID): String {
        val totrinnskontrollSaksbehandler =
            behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId, StegType.SEND_TIL_BESLUTTER)

        return totrinnskontrollSaksbehandler?.opprettetAv ?: throw Feil("Fant ikke saksbehandler som sendte til beslutter")
    }

    fun hentBeslutter(behandlingId: UUID): String? {
        return behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId, StegType.BESLUTTE_VEDTAK)
            ?.opprettetAv
            ?.takeIf { NAVIDENT_REGEX.matches(it) }
    }

    fun hentTotrinnskontrollStatus(behandlingId: UUID): StatusTotrinnskontrollDto {
        val behandlingStatus = behandlingService.hentBehandling(behandlingId).status

        if (behandlingErGodkjentEllerOpprettet(behandlingStatus)) {
            return StatusTotrinnskontrollDto(TotrinnkontrollStatus.UAKTUELT)
        }

        return when (behandlingStatus) {
            BehandlingStatus.FATTER_VEDTAK -> finnStatusForVedtakSomSkalFattes(behandlingId)
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
    private fun finnStatusForVedtakSomSkalFattes(behandlingId: UUID): StatusTotrinnskontrollDto {
        val historikkHendelse = behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId)
        if (historikkHendelse.steg != StegType.SEND_TIL_BESLUTTER) {
            throw Feil(
                message = "Siste historikken har feil steg, steg=${historikkHendelse.steg}",
                frontendFeilmelding = "Feil i historikken, kontakt brukerstøtte id=$behandlingId",
            )
        }
        return if (beslutterErLikBehandler(historikkHendelse) || !tilgangService.harTilgangTilRolle(BehandlerRolle.BESLUTTER)) {
            StatusTotrinnskontrollDto(
                TotrinnkontrollStatus.IKKE_AUTORISERT,
                TotrinnskontrollDto(historikkHendelse.opprettetAvNavn, historikkHendelse.endretTid),
            )
        } else {
            StatusTotrinnskontrollDto(TotrinnkontrollStatus.KAN_FATTE_VEDTAK)
        }
    }

    /**
     * Hvis behandlingen utredes sjekkes det for om det finnes ett tidligere beslutt, som då kun kan være underkjent
     */
    private fun finnStatusForVedtakSomErFattet(behandlingId: UUID): StatusTotrinnskontrollDto {
        val besluttetVedtakHendelse =
            behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId, StegType.BESLUTTE_VEDTAK)
                ?: return StatusTotrinnskontrollDto(TotrinnkontrollStatus.UAKTUELT)
        return when (besluttetVedtakHendelse.utfall) {
            StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT -> {
                if (besluttetVedtakHendelse.metadata == null) {
                    throw Feil(
                        message = "Har underkjent vedtak - savner metadata",
                        frontendFeilmelding = "Savner metadata, kontakt brukerstøtte id=$behandlingId",
                    )
                }
                val beslutt = objectMapper.readValue<BeslutteVedtakDto>(besluttetVedtakHendelse.metadata.json)
                StatusTotrinnskontrollDto(
                    TotrinnkontrollStatus.TOTRINNSKONTROLL_UNDERKJENT,
                    TotrinnskontrollDto(
                        besluttetVedtakHendelse.opprettetAvNavn,
                        besluttetVedtakHendelse.endretTid,
                        beslutt.godkjent,
                        beslutt.begrunnelse,
                        beslutt.årsak,
                    ),
                )
            }
            StegUtfall.ANGRE_SEND_TIL_BESLUTTER -> StatusTotrinnskontrollDto(TotrinnkontrollStatus.UAKTUELT)
            else -> error(
                "Skal ikke kunne være annen status enn UNDERKJENT når " +
                    "behandligStatus!=${BehandlingStatus.FATTER_VEDTAK}",
            )
        }
    }

    private fun beslutterErLikBehandler(beslutteVedtakHendelse: Behandlingshistorikk) =
        SikkerhetContext.hentSaksbehandlerEllerSystembruker() == beslutteVedtakHendelse.opprettetAv
}
