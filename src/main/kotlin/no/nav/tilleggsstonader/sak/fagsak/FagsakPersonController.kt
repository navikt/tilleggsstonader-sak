package no.nav.tilleggsstonader.sak.fagsak

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.fagsak.dto.FagsakPersonDto
import no.nav.tilleggsstonader.sak.fagsak.dto.FagsakPersonUtvidetDto
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/fagsak-person"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakPersonController(
    private val tilgangService: TilgangService,
    private val fagsakPersonService: FagsakPersonService,
    private val fagsakService: FagsakService,
) {

    @GetMapping("{fagsakPersonId}")
    fun hentFagsakPerson(@PathVariable fagsakPersonId: UUID): FagsakPersonDto {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        val person = fagsakPersonService.hentPerson(fagsakPersonId)
        val fagsaker = fagsakService.finnFagsakerForFagsakPersonId(person.id)
        return FagsakPersonDto(
            id = person.id,
            tilsynBarn = fagsaker.barnetilsyn?.id,
        )
    }

    @GetMapping("{fagsakPersonId}/utvidet")
    fun hentFagsakPersonUtvidet(@PathVariable fagsakPersonId: UUID): FagsakPersonUtvidetDto {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        val person = fagsakPersonService.hentPerson(fagsakPersonId)
        val fagsaker = fagsakService.finnFagsakerForFagsakPersonId(person.id)
        return FagsakPersonUtvidetDto(
            person.id,
            tilsynBarn = fagsaker.barnetilsyn?.let { fagsakService.fagsakTilDto(it) },
        )
    }
}
