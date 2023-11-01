package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.StatusTotrinnskontrollDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

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
    fun hentTotrinnskontroll(@PathVariable behandlingId: UUID): StatusTotrinnskontrollDto {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return totrinnskontrollService.hentTotrinnskontrollStatus(behandlingId)
    }

    @PostMapping("/{behandlingId}/send-til-beslutter")
    fun sendTilBeslutter(
        @PathVariable behandlingId: UUID,
    ): StatusTotrinnskontrollDto {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        stegService.håndterSteg(behandlingId, sendTilBeslutterSteg)
        return totrinnskontrollService.hentTotrinnskontrollStatus(behandlingId)
    }

    @PostMapping("/{behandlingId}/beslutte-vedtak")
    fun beslutteVedtak(
        @PathVariable behandlingId: UUID,
        @RequestBody request: BeslutteVedtakDto,
    ): StatusTotrinnskontrollDto {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        stegService.håndterSteg(behandlingId, beslutteVedtakSteg, request)
        return totrinnskontrollService.hentTotrinnskontrollStatus(behandlingId)
    }

    @PostMapping("/{behandlingId}/angre-send-til-beslutter")
    fun angreSendTilBeslutter(@PathVariable behandlingId: UUID): StatusTotrinnskontrollDto {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        angreSendTilBeslutterService.angreSendTilBeslutter(behandlingId)
        return totrinnskontrollService.hentTotrinnskontrollStatus(behandlingId)
    }
}
