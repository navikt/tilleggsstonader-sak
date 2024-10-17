package no.nav.tilleggsstonader.sak.opplysninger

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.opplysninger.dto.PersonopplysningerDto
import no.nav.tilleggsstonader.sak.opplysninger.pdl.logger
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/personopplysninger"])
@ProtectedWithClaims(issuer = "azuread")
class PersonopplysningController(
    private val tilgangService: TilgangService,
    private val personopplysningerService: PersonopplysningerService,
) {

    @GetMapping("{behandlingId}")
    fun hentPersonopplysninger(@PathVariable behandlingId: BehandlingId): PersonopplysningerDto {
        logger.info("Du har kommet til /api/personopplysninger/behandling/{fagsakPersonId}")
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return personopplysningerService.hentPersonopplysninger(behandlingId)
    }

    @GetMapping("fagsak-person/{fagsakPersonId}")
    fun hentPersonopplysningerForPerson(@PathVariable fagsakPersonId: FagsakPersonId): PersonopplysningerDto {
        logger.info("Du har kommet til /api/personopplysninger/fagsak-person/{fagsakPersonId}")
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return personopplysningerService.hentPersonopplysningerForFagsakPerson(fagsakPersonId)
    }
}
