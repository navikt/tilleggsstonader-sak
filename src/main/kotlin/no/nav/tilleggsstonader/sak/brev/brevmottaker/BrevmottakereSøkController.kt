package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.felles.PersonIdentDto
import no.nav.tilleggsstonader.sak.opplysninger.ereg.EregService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/brevmottakere"])
@ProtectedWithClaims(issuer = "azuread")
class BrevmottakereSøkController(
    private val tilgangService: TilgangService,
    private val personService: PersonService,
    private val eregService: EregService,
) {

    @PostMapping("person")
    fun søkPerson(
        @RequestBody personIdentDto: PersonIdentDto,
    ): PersonTreffDto {
        val personIdent = personIdentDto.personIdent
        brukerfeilHvisIkke(FNR_REGEX.matches(personIdent)) { "Ugyldig fødselsnummer" }
        tilgangService.validerTilgangTilPersonMedBarn(personIdent, AuditLoggerEvent.ACCESS)
        return PersonTreffDto(personIdent, personService.hentVisningsnavnForPerson(personIdent))
    }

    @GetMapping("organisasjon/{organisasjonsnummer}")
    fun søkOrganisasjon(
        @PathVariable organisasjonsnummer: String,
    ): IOrganisasjonDto {
        if (!ORGNR_REGEX.matches(organisasjonsnummer)) {
            throw ApiFeil("Ugyldig organisasjonsnummer", HttpStatus.BAD_REQUEST)
        }
        val organisasjonsNavnDto = eregService.hentOrganisasjon(organisasjonsnummer)
        return IOrganisasjonDto(organisasjonsNavnDto.navn.navnelinje1, organisasjonsNavnDto.organisasjonsnummer)
    }

    companion object {

        private val ORGNR_REGEX = """\d{9}""".toRegex()
        private val FNR_REGEX = """\d{11}""".toRegex()
    }

    data class PersonTreffDto(val ident: String, val navn: String)

    data class IOrganisasjonDto(val navn: String?, val organisasjonsnummer: String)
}
