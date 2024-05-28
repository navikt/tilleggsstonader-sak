package no.nav.tilleggsstonader.sak.opplysninger.aktivitet

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/aktivitet"])
@ProtectedWithClaims(issuer = "azuread")
class AktivitetController(
    private val tilgangService: TilgangService,
    private val aktivitetService: AktivitetService,
    private val behandlingService: BehandlingService,
) {

    @GetMapping("{fagsakPersonId}")
    fun hentAktiviteter(
        @PathVariable fagsakPersonId: UUID,
    ): List<AktivitetArenaDto> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return aktivitetService.hentAktiviteter(fagsakPersonId)
    }

    @Deprecated("Skal fjernes. aktivitet skal hentes med vilkårperioder") // TODO: Fjern når aktivitet hentes med vilkårperioder
    @GetMapping("/behandling/{behandlingId}")
    fun hentAktivitetForBehandling(
        @PathVariable behandlingId: UUID,
    ): List<AktivitetArenaDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        return aktivitetService.hentAktiviteter(saksbehandling.fagsakPersonId)
    }
}
