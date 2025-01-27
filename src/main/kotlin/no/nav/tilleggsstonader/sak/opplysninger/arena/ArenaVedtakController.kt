package no.nav.tilleggsstonader.sak.opplysninger.arena

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.arena.vedtak.ArenaSakOgVedtakDto
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/arena/vedtak")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ArenaVedtakController(
    private val arenaService: ArenaService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("{fagsakPersonId}")
    fun hentVedtak(
        @PathVariable fagsakPersonId: FagsakPersonId,
    ): ArenaSakOgVedtakDto {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return arenaService.hentVedtak(fagsakPersonId)
    }
}
