package no.nav.tilleggsstonader.sak.behandling

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingDto
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.behandling.dto.tilDto
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/behandling"])
@ProtectedWithClaims(issuer = "azuread")
class BehandlingController(
    private val behandlingService: BehandlingService,
    // private val behandlingPåVentService: BehandlingPåVentService,
    private val fagsakService: FagsakService,
    private val henleggService: HenleggService,
    private val tilgangService: TilgangService,
    // private val gjenbrukVilkårService: GjenbrukVilkårService,
) {

    @GetMapping("{behandlingId}")
    fun hentBehandling(@PathVariable behandlingId: UUID): BehandlingDto {
        val saksbehandling: Saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        tilgangService.validerTilgangTilPersonMedBarn(saksbehandling.ident, AuditLoggerEvent.ACCESS)
        return saksbehandling.tilDto()
    }

    @GetMapping("gamle-behandlinger")
    fun hentGamleUferdigeBehandlinger(): List<BehandlingDto> {
        val stønadstyper = Stønadstype.values()
        val gamleBehandlinger = stønadstyper.flatMap { stønadstype ->
            behandlingService.hentUferdigeBehandlingerOpprettetFørDato(stønadstype).map { it.tilDto(stønadstype) }
        }
        return gamleBehandlinger
    }

    /*@PostMapping("{behandlingId}/vent")
    fun settPåVent(
        @PathVariable behandlingId: UUID,
        @RequestBody settPåVentRequest: SettPåVentRequest,
    ): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        behandlingPåVentService.settPåVent(behandlingId, settPåVentRequest)

        return Ressurs.success(behandlingId)
    }

    @GetMapping("{behandlingId}/kan-ta-av-vent")
    fun kanTaAvVent(@PathVariable behandlingId: UUID): Ressurs<TaAvVentStatusDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarSaksbehandlerrolle()
        return Ressurs.success(behandlingPåVentService.kanTaAvVent(behandlingId))
    }

    @PostMapping("{behandlingId}/aktiver")
    fun taAvVent(@PathVariable behandlingId: UUID): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        behandlingPåVentService.taAvVent(behandlingId)
        return Ressurs.success(behandlingId)
    }
     */

    @PostMapping("{behandlingId}/henlegg")
    fun henleggBehandling(@PathVariable behandlingId: UUID, @RequestBody henlagt: HenlagtDto): BehandlingDto {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        val henlagtBehandling = henleggService.henleggBehandling(behandlingId, henlagt)
        val fagsak: Fagsak = fagsakService.hentFagsak(henlagtBehandling.fagsakId)
        return henlagtBehandling.tilDto(fagsak.stønadstype)
    }

    @GetMapping("/ekstern/{eksternBehandlingId}")
    fun hentBehandling(@PathVariable eksternBehandlingId: Long): BehandlingDto {
        val saksbehandling = behandlingService.hentSaksbehandling(eksternBehandlingId)
        tilgangService.validerTilgangTilPersonMedBarn(saksbehandling.ident, AuditLoggerEvent.ACCESS)
        return saksbehandling.tilDto()
    }

    /*
    @GetMapping("/gjenbruk/{behandlingId}")
    fun hentBehandlingForGjenbrukAvVilkår(@PathVariable behandlingId: UUID): List<BehandlingDto> {
        //tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        //tilgangService.validerHarSaksbehandlerrolle()
        return Ressurs.success(gjenbrukVilkårService.finnBehandlingerForGjenbruk(behandlingId))
    }
     */
}
