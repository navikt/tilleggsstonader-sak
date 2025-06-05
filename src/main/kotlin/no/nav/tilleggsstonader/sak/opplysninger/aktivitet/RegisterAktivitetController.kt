package no.nav.tilleggsstonader.sak.opplysninger.aktivitet

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/register-aktivitet"])
@ProtectedWithClaims(issuer = "azuread")
class RegisterAktivitetController(
    private val tilgangService: TilgangService,
    private val registerAktivitetService: RegisterAktivitetService,
) {
    @GetMapping("{fagsakPersonId}")
    fun hentAktiviteter(
        @PathVariable fagsakPersonId: FagsakPersonId,
    ): RegisterAktiviteterDto {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return registerAktivitetService.hentAktiviteterMedPerioder(fagsakPersonId)
    }
}
