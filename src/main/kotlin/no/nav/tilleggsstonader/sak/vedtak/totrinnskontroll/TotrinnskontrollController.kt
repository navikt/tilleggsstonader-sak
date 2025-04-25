package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.SendTilBeslutterRequest
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.StatusTotrinnskontrollDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/totrinnskontroll")
@ProtectedWithClaims(issuer = "azuread")
class TotrinnskontrollController(
    private val tilgangService: TilgangService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val stegService: StegService,
    private val beslutteVedtakSteg: BeslutteVedtakSteg,
    private val sendTilBeslutterSteg: SendTilBeslutterSteg,
    private val angreSendTilBeslutterService: AngreSendTilBeslutterService,
) {
    @GetMapping("{behandlingId}")
    fun hentTotrinnskontroll(
        @PathVariable behandlingId: BehandlingId,
    ): StatusTotrinnskontrollDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return totrinnskontrollService.hentTotrinnskontrollStatus(behandlingId)
    }

    @PostMapping("/{behandlingId}/send-til-beslutter")
    fun sendTilBeslutter(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody request: SendTilBeslutterRequest,
    ): StatusTotrinnskontrollDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        stegService.håndterSteg(behandlingId, sendTilBeslutterSteg, request)
        return totrinnskontrollService.hentTotrinnskontrollStatus(behandlingId)
    }

    @PostMapping("/{behandlingId}/beslutte-vedtak")
    fun beslutteVedtak(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody request: BeslutteVedtakDto,
    ): StatusTotrinnskontrollDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        stegService.håndterSteg(behandlingId, beslutteVedtakSteg, request)
        return totrinnskontrollService.hentTotrinnskontrollStatus(behandlingId)
    }

    @PostMapping("/{behandlingId}/angre-send-til-beslutter")
    fun angreSendTilBeslutter(
        @PathVariable behandlingId: BehandlingId,
    ): StatusTotrinnskontrollDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        angreSendTilBeslutterService.angreSendTilBeslutter(behandlingId)
        return totrinnskontrollService.hentTotrinnskontrollStatus(behandlingId)
    }
}
