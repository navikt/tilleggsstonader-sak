package no.nav.tilleggsstonader.sak.tilgang

import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.AdRolle
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.RolleConfig
import no.nav.tilleggsstonader.sak.opplysninger.egenansatt.EgenAnsattService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.domain.AdressebeskyttelseForPersonMedRelasjoner
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import org.springframework.stereotype.Service

/**
 * Modifisert versjon fra familie-integrasjoner. Denne har ikke med kontroll av annen forelder eller andre relasjoner
 * Hvis man eks skal vise barns andre forelder eller sivilstandsrelasjoner så burde vi legge til tilgangskontroll av de
 */
@Service
class TilgangskontrollService(
    private val egenAnsattService: EgenAnsattService,
    private val personService: PersonService,
    rolleConfig: RolleConfig,
) {
    private val adRoller = rolleConfig.rollerMedBeskrivelse

    /**
     * Sjekker tilgang til person.
     * Noter at denne ikke skal brukes per default. Bruk [sjekkTilgangTilPersonMedRelasjoner]
     * Bruk kun denne hvis man skal sjekke tilgang person
     */
    fun sjekkTilgang(
        personIdent: String,
        jwtToken: JwtToken,
    ): Tilgang {
        val adressebeskyttelse = personService.hentAdressebeskyttelse(personIdent)
        secureLogger.info("Sjekker tilgang til $personIdent")
        return hentTilgang(adressebeskyttelse, jwtToken, personIdent) { egenAnsattService.erEgenAnsatt(personIdent) }
    }

    /**
     * Når vi sjekker tilgang til person med relasjoner vet vi ikke hvilke barn som er relevante
     * Då kontrolleres alle barnen til bruker og alle andre foreldre
     */
    fun sjekkTilgangTilPersonMedRelasjoner(
        personIdent: String,
        jwtToken: JwtToken,
    ): Tilgang {
        val personMedRelasjoner = personService.hentAdressebeskyttelseForPersonOgRelasjoner(personIdent)
        secureLogger.info("Sjekker tilgang til $personMedRelasjoner")

        val høyesteGraderingen = TilgangskontrollUtil.høyesteGraderingen(personMedRelasjoner)
        return hentTilgang(høyesteGraderingen, jwtToken, personIdent) { erEgenAnsatt(personMedRelasjoner) }
    }

    private fun hentTilgang(
        adressebeskyttelsegradering: AdressebeskyttelseGradering,
        jwtToken: JwtToken,
        personIdent: String,
        egenAnsattSjekk: () -> Boolean,
    ): Tilgang {
        val tilgang =
            when (adressebeskyttelsegradering) {
                AdressebeskyttelseGradering.FORTROLIG -> hentTilgangForRolle(adRoller.kode7, jwtToken, personIdent)
                AdressebeskyttelseGradering.STRENGT_FORTROLIG, AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND ->
                    hentTilgangForRolle(adRoller.kode6, jwtToken, personIdent)

                else -> Tilgang(harTilgang = true)
            }
        if (!tilgang.harTilgang) {
            return tilgang
        }
        if (egenAnsattSjekk()) {
            return hentTilgangForRolle(adRoller.egenAnsatt, jwtToken, personIdent)
        }
        return Tilgang(harTilgang = true)
    }

    /**
     * Trenger ikke å sjekke barn, men muligens andre forelderen
     */
    private fun erEgenAnsatt(adressebeskyttelseForPersonMedRelasjoner: AdressebeskyttelseForPersonMedRelasjoner): Boolean {
        val relevanteIdenter = adressebeskyttelseForPersonMedRelasjoner.identerForEgenAnsattKontroll()

        return egenAnsattService.erEgenAnsatt(relevanteIdenter).any { it.value.erEgenAnsatt }
    }

    private fun hentTilgangForRolle(
        adRolle: AdRolle?,
        jwtToken: JwtToken,
        personIdent: String,
    ): Tilgang {
        val grupper = jwtToken.jwtTokenClaims.getAsList("groups")
        if (grupper.any { it == adRolle?.rolleId }) {
            return Tilgang(true)
        }
        secureLogger.info(
            "${jwtToken.jwtTokenClaims.get("preferred_username")} " +
                "har ikke tilgang ${adRolle?.beskrivelse} for $personIdent",
        )
        return Tilgang(harTilgang = false, begrunnelse = adRolle?.beskrivelse)
    }
}
