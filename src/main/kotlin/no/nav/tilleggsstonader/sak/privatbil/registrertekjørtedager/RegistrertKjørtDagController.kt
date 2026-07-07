package no.nav.tilleggsstonader.sak.privatbil.registrertekjørtedager

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/registrert-kjort-dag")
@ProtectedWithClaims(issuer = "azuread")
class RegistrertKjørtDagController(
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val registrertKjørtDagService: RegistrertKjørtDagService,
) {
    @GetMapping("{behandlingId}")
    fun hentForBehandling(
        @PathVariable behandlingId: BehandlingId,
    ): List<RegistrertKjørtUke> {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerLesetilgangTilBehandling(behandlingId)
        return registrertKjørtDagService.hentForBehandling(behandlingId)
    }

    @PostMapping("{behandlingId}")
    fun lagreUke(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody request: RegistrertKjørtUkePostRequest,
    ): RegistrertKjørtUke {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        behandlingService.markerBehandlingSomPåbegyntHvisDenHarStatusOpprettet(behandlingId)
        return registrertKjørtDagService.lagreUke(behandlingId, request)
    }

    @PutMapping("{behandlingId}/{ukeId}")
    fun oppdaterUke(
        @PathVariable behandlingId: BehandlingId,
        @PathVariable ukeId: UUID,
        @RequestBody request: RegistrertKjørtUkePutRequest,
    ): RegistrertKjørtUke {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        behandlingService.markerBehandlingSomPåbegyntHvisDenHarStatusOpprettet(behandlingId)
        return registrertKjørtDagService.oppdaterUke(behandlingId, ukeId, request)
    }
}
