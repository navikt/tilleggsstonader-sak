package no.nav.tilleggsstonader.sak.vedlegg

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedlegg")
@ProtectedWithClaims(issuer = "azuread")
class VedleggController(
    private val tilgangService: TilgangService,
    private val vedleggService: VedleggService,
) {
    @PostMapping("/fagsak-person/{fagsakPersonId}")
    fun finnVedleggForBruker(
        @PathVariable fagsakPersonId: FagsakPersonId,
        @RequestBody vedleggRequest: VedleggRequest,
    ): List<DokumentInfoDto> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return vedleggService.finnVedleggForBruker(fagsakPersonId, vedleggRequest)
    }
}
