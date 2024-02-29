package no.nav.tilleggsstonader.sak.fagsak

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.fagsak.dto.FagsakDto
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/fagsak"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(
    private val fagsakService: FagsakService,
    private val tilgangService: TilgangService,
) {

    @GetMapping("{fagsakId}")
    fun hentFagsak(@PathVariable fagsakId: UUID): FagsakDto {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)
        return fagsakService.hentFagsakMedBehandlinger(fagsakId)
    }

    @GetMapping("/ekstern/{eksternFagsakId}")
    fun hentFagsak(@PathVariable eksternFagsakId: Long): FagsakDto {
        val fagsakDto = fagsakService.hentFagsakDtoPåEksternId(eksternFagsakId)
        tilgangService.validerTilgangTilFagsak(fagsakDto.id, AuditLoggerEvent.ACCESS)
        return fagsakDto
    }
}
