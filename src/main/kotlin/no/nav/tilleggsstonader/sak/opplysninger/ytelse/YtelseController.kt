package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/ytelse"])
@ProtectedWithClaims(issuer = "azuread")
class YtelseController(
    private val tilgangService: TilgangService,
    private val aktivitetService: YtelseService,
) {

    @GetMapping("{fagsakPersonId}")
    fun hentYtelser(
        @PathVariable fagsakPersonId: UUID,
    ): YtelserRegisterDto {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return aktivitetService.hentYtelser(fagsakPersonId)
    }

    @GetMapping("/behandling/{behandlingId}")
    fun hentYtelserForBehandling(
        @PathVariable behandlingId: UUID,
    ): YtelserRegisterDto {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return aktivitetService.hentYtelserForBehandling(behandlingId)
    }
}
