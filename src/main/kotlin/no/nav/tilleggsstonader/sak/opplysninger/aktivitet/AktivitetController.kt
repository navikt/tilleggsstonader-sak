package no.nav.tilleggsstonader.sak.opplysninger.aktivitet

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetDto
import no.nav.tilleggsstonader.kontrakter.felles.IdentRequest
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/aktivitet"])
@ProtectedWithClaims(issuer = "azuread")
class AktivitetController(
    private val tilgangService: TilgangService,
    private val fagsakPersonService: FagsakPersonService,
    private val aktivitetClient: AktivitetClient,
) {

    @PostMapping("finn")
    fun finnAktiviteter(
        @RequestBody identRequest: IdentRequest,
    ): List<AktivitetDto> {
        tilgangService.validerTilgangTilPersonMedBarn(identRequest.ident, AuditLoggerEvent.ACCESS)
        return aktivitetClient.hentAktiviteter(identRequest.ident, LocalDate.now().minusYears(3), null)
    }

    @PostMapping("{fagsakPersonId}")
    fun hentAktiviteter(
        @PathVariable fagsakPersonId: UUID,
    ): List<AktivitetDto> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        val ident = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        return aktivitetClient.hentAktiviteter(ident, LocalDate.now().minusYears(3), null)
    }
}
