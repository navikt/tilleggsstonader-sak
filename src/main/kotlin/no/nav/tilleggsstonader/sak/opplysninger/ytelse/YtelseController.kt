package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/ytelse"])
@ProtectedWithClaims(issuer = "azuread")
class YtelseController(
    private val tilgangService: TilgangService,
    private val aktivitetService: YtelseService,
) {
    @GetMapping("{fagsakPersonId}")
    fun hentYtelser(
        @PathVariable fagsakPersonId: FagsakPersonId,
    ): YtelserRegisterDto {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return aktivitetService.hentYtelser(fagsakPersonId)
    }
}
