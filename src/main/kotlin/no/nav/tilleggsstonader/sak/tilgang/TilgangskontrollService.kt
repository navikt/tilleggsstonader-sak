package no.nav.tilleggsstonader.sak.tilgang

import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.AdRolle
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.RolleConfig
import no.nav.tilleggsstonader.sak.opplysninger.egenansatt.EgenAnsattService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gradering
import org.springframework.cache.annotation.Cacheable
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

    @Cacheable(
        cacheNames = ["TILGANG_TIL_BRUKER"],
        key = "#jwtToken.subject.concat(#personIdent)",
        condition = "#personIdent != null && #jwtToken.subject != null",
    )
    fun sjekkTilgang(
        personIdent: String,
        jwtToken: JwtToken,
    ): Tilgang {
        val adressebeskyttelse =
            personService
                .hentPersonKortBolk(listOf(personIdent))
                .values
                .single()
                .adressebeskyttelse
                .gradering()
        return hentTilgang(adressebeskyttelse, jwtToken, personIdent) { egenAnsattService.erEgenAnsatt(personIdent) }
    }

    @Cacheable(
        cacheNames = ["TILGANG_TIL_PERSON_MED_RELASJONER"],
        key = "#jwtToken.subject.concat(#personIdent)",
        condition = "#jwtToken.subject != null",
    )
    fun sjekkTilgangTilPersonMedRelasjoner(
        personIdent: String,
        jwtToken: JwtToken,
    ): Tilgang {
        val personMedRelasjoner = hentPersonMedRelasjoner(personIdent)
        secureLogger.info("Sjekker tilgang til {}", personMedRelasjoner)

        val høyesteGraderingen = TilgangskontrollUtil.høyesteGraderingen(personMedRelasjoner)
        return hentTilgang(høyesteGraderingen, jwtToken, personIdent) { erEgenAnsatt(personMedRelasjoner) }
    }

    private fun hentPersonMedRelasjoner(personIdent: String): PersonMedRelasjoner {
        val søkerMedBarn = personService.hentPersonMedBarn(personIdent)
        val barn =
            søkerMedBarn.barn.map { PersonMedAdresseBeskyttelse(it.key, it.value.adressebeskyttelse.gradering()) }

        return PersonMedRelasjoner(
            personIdent = søkerMedBarn.søkerIdent,
            adressebeskyttelse = søkerMedBarn.søker.adressebeskyttelse.gradering(),
            barn = barn,
        )
    }

    private fun hentTilgang(
        adressebeskyttelsegradering: AdressebeskyttelseGradering?,
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
    fun erEgenAnsatt(personMedRelasjoner: PersonMedRelasjoner): Boolean {
        val relevanteIdenter = setOf(personMedRelasjoner.personIdent)

        return egenAnsattService.erEgenAnsatt(relevanteIdenter).any { it.value }
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
