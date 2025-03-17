package no.nav.tilleggsstonader.sak.fagsak.søk

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.infrastruktur.felles.PersonIdentDto
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.util.FnrUtil.validerIdent
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/sok"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SøkController(
    private val søkService: SøkService,
    private val personService: PersonService,
    private val tilgangService: TilgangService,
) {
    @PostMapping("", "/person")
    fun søkPerson(
        @RequestBody personIdentRequest: PersonIdentDto,
    ): Søkeresultat {
        validerPersonIdent(personIdentRequest)
        val personIdenter = hentOgValiderAtIdentEksisterer(personIdentRequest)
        tilgangService.validerTilgangTilPersonMedRelasjoner(personIdentRequest.personIdent, AuditLoggerEvent.ACCESS)
        return søkService.søkPerson(personIdenter)
    }

    @GetMapping("/person/fagsak-ekstern/{eksternFagsakId}")
    fun søkPerson(
        @PathVariable eksternFagsakId: Long,
    ): Søkeresultat {
        val søkeresultat = søkService.søkPersonForEksternFagsak(eksternFagsakId)
        søkeresultat.fagsakPersonId?.let {
            tilgangService.validerTilgangTilFagsakPerson(it, AuditLoggerEvent.ACCESS)
        }
        return søkeresultat
    }

    // brukes til brev, burde vi flytte den til brev?
    @PostMapping("/person/uten-fagsak")
    fun søkPersonUtenFagsak(
        @RequestBody personIdentRequest: PersonIdentDto,
    ): SøkeresultatUtenFagsak {
        validerPersonIdent(personIdentRequest)
        tilgangService.validerTilgangTilPerson(personIdentRequest.personIdent, AuditLoggerEvent.ACCESS)

        return søkService.søkPersonUtenFagsak(personIdentRequest.personIdent)
    }

    private fun validerPersonIdent(personIdentRequest: PersonIdentDto) {
        validerIdent(personIdentRequest.personIdent)
    }

    private fun hentOgValiderAtIdentEksisterer(personIdentRequest: PersonIdentDto): PdlIdenter =
        personService.hentPersonIdenter(personIdentRequest.personIdent)
}
