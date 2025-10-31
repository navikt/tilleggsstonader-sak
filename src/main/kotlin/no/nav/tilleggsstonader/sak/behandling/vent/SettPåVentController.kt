package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/sett-pa-vent"])
@ProtectedWithClaims(issuer = "azuread")
class SettPåVentController(
    private val tilgangService: TilgangService,
    private val settPåVentService: SettPåVentService,
    private val taAvVentService: TaAvVentService,
) {
    @GetMapping("{behandlingId}")
    fun hentStatusSettPåVent(
        @PathVariable behandlingId: BehandlingId,
    ): StatusPåVentDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return settPåVentService.hentStatusSettPåVent(behandlingId)
    }

    @PostMapping("{behandlingId}")
    fun settPåVent(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody dto: SettPåVentDto,
    ): StatusPåVentDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        return settPåVentService.settPåVent(behandlingId, dto.tilDomene())
    }

    @PutMapping("{behandlingId}")
    fun oppdaterSettPåVent(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody dto: OppdaterSettPåVentDto,
    ): StatusPåVentDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        return settPåVentService.oppdaterSettPåVent(behandlingId, dto)
    }

    @DeleteMapping("{behandlingId}")
    fun taAvVent(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody taAvVentDto: TaAvVentDto,
    ) {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        taAvVentService.taAvVent(behandlingId, taAvVentDto)
    }

    @GetMapping("{behandlingId}/kan-ta-av-vent")
    fun kanTaAvVent(
        @PathVariable behandlingId: BehandlingId,
    ): KanTaAvVentDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarSaksbehandlerrolle()
        return taAvVentService.kanTaAvVent(behandlingId)
    }
}
