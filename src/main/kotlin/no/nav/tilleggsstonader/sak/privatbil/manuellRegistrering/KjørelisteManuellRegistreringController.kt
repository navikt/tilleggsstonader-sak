package no.nav.tilleggsstonader.sak.privatbil.manuellRegistrering

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/kjoreliste/manuell-registrering"])
@ProtectedWithClaims(issuer = "azuread")
class KjørelisteManuellRegistreringController(
    private val tilgangService: TilgangService,
    private val kjørelisteManuellRegistreringService: KjørelisteManuellRegistreringService,
) {
    @GetMapping("{behandlingId}")
    fun hentKjørelisteOversikt(
        @PathVariable behandlingId: BehandlingId,
    ): KjørelisteOversiktDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerLesetilgangTilBehandling(behandlingId)

        // TODO: Kutt ut fremtidige uker
        return kjørelisteManuellRegistreringService.hentKjørelisteOversikt(behandlingId)
    }

    @PostMapping("{behandlingId}")
    fun lagreManuellKjøreliste(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody request: LagreManuellKjørelisteRequest,
    ) {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)

        kjørelisteManuellRegistreringService.lagreManuellKjøreliste(behandlingId, request)
    }
}

data class LagreManuellKjørelisteRequest(
    val journalpostId: String,
    val reiseId: ReiseId,
    val begrunnelse: String?,
    val reisedager: List<KjørelisteDag>,
)
