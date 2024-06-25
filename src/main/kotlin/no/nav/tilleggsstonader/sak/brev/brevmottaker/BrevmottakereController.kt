package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.familie.prosessering.rest.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.infrastruktur.felles.PersonIdentDto
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping(path = ["/api/brevmottakere/"])
@ProtectedWithClaims(issuer = "azuread")
class BrevmottakereController(
    private val tilgangService: TilgangService,
    private val brevmottakereService: BrevmottakereService,
    private val personService: PersonService,
) {

    @GetMapping("/{behandlingId}")
    fun hentBrevmottakere(@PathVariable behandlingId: UUID): BrevmottakereDto {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)

        return brevmottakereService.hentEllerOpprettBrevmottakere(behandlingId)
    }

    @PostMapping("/{behandlingId}")
    fun velgBrevmottakere(
        @PathVariable behandlingId: UUID,
        @RequestBody brevmottakere: BrevmottakereDto,
    ) {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return brevmottakereService.lagreBrevmottakere(behandlingId, brevmottakere)
    }

    @PostMapping("person")
    fun søkPerson(
        @RequestBody personIdentDto: PersonIdentDto,
    ): Ressurs<PersonTreffDto> {
        val personIdent = personIdentDto.personIdent
        tilgangService.validerTilgangTilPersonMedBarn(personIdent, AuditLoggerEvent.ACCESS)
        val result = PersonTreffDto(personIdent, personService.hentVisningsnavnForPerson(personIdent))
        return Ressurs.success(result)
    }

    data class PersonTreffDto(val ident: String, val navn: String)
}
