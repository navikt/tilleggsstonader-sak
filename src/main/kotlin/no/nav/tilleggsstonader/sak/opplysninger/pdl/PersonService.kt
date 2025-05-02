package no.nav.tilleggsstonader.sak.opplysninger.pdl

import no.nav.tilleggsstonader.kontrakter.pdl.GeografiskTilknytningDto
import no.nav.tilleggsstonader.libs.spring.cache.getCachedOrLoad
import no.nav.tilleggsstonader.libs.spring.cache.getNullable
import no.nav.tilleggsstonader.libs.spring.cache.getValue
import no.nav.tilleggsstonader.sak.opplysninger.dto.SøkerMedBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.domain.AdressebeskyttelseForPersonMedRelasjoner
import no.nav.tilleggsstonader.sak.opplysninger.pdl.domain.AdressebeskyttelseForPersonUtenRelasjoner
import no.nav.tilleggsstonader.sak.opplysninger.pdl.domain.PersonMedAdresseBeskyttelse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.domain.tilPersonMedAdresseBeskyttelse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Familierelasjonsrolle
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlAnnenForelder
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdentGruppe
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonKort
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlSøker
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gradering
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.visningsnavn
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class PersonService(
    private val pdlClient: PdlClient,
    private val cacheManager: CacheManager,
) {
    fun hentSøker(ident: String): PdlSøker = cacheManager.getValue("personService_hentsoker", ident) { pdlClient.hentSøker(ident) }

    fun hentPersonUtenBarn(ident: String): SøkerMedBarn {
        val søker = hentSøker(ident)
        return SøkerMedBarn(ident, søker, emptyMap())
    }

    fun hentPersonMedBarn(ident: String): SøkerMedBarn {
        val søker = hentSøker(ident)
        val barnIdentifikatorer =
            søker.forelderBarnRelasjon
                .filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
                .mapNotNull { it.relatertPersonsIdent }
        return SøkerMedBarn(ident, søker, hentBarn(barnIdentifikatorer))
    }

    fun hentAdressebeskyttelse(ident: String): AdressebeskyttelseForPersonUtenRelasjoner =
        hentPersonKortBolk(listOf(ident))
            .values
            .single()
            .adressebeskyttelse
            .gradering()
            .let {
                val søker = PersonMedAdresseBeskyttelse(ident, it)
                AdressebeskyttelseForPersonUtenRelasjoner(søker = søker)
            }

    fun hentAdressebeskyttelseForPersonOgRelasjoner(ident: String): AdressebeskyttelseForPersonMedRelasjoner {
        val søkerMedBarn = hentPersonMedBarn(ident)
        val barn = søkerMedBarn.barn
        val andreForeldreIdenter = barn.identerAndreForeldre(identSøker = ident)
        val andreForeldre = hentPersonKortBolk(andreForeldreIdenter)

        return AdressebeskyttelseForPersonMedRelasjoner(
            søker =
                PersonMedAdresseBeskyttelse(
                    personIdent = ident,
                    adressebeskyttelse = søkerMedBarn.søker.adressebeskyttelse.gradering(),
                ),
            barn = barn.tilPersonMedAdresseBeskyttelse(),
            andreForeldre = andreForeldre.tilPersonMedAdresseBeskyttelse(),
        )
    }

    fun hentBarn(barnIdentifikatorer: List<String>) =
        cacheManager.getCachedOrLoad("personService_hentBarn", barnIdentifikatorer) {
            pdlClient.hentBarn(it.toList())
        }

    fun hentAndreForeldre(personIdenter: List<String>): Map<String, PdlAnnenForelder> = pdlClient.hentAndreForeldre(personIdenter)

    fun hentFolkeregisterIdenter(ident: String): PdlIdenter = hentIdenterCached(ident).folkeregisteridenter()

    fun hentFolkeregisterOgNpidIdenter(ident: String): PdlIdenter =
        hentIdenterCached(ident).medIdentgrupper(PdlIdentGruppe.FOLKEREGISTERIDENT.name, PdlIdentGruppe.NPID.name)

    private fun hentIdenterCached(ident: String): PdlIdenter =
        cacheManager.getValue("personidenter", ident) {
            pdlClient.hentPersonidenter(ident)
        }

    fun hentIdenterBolk(identer: List<String>): Map<String, PdlIdent> = pdlClient.hentIdenterBolk(identer)

    /**
     * PDL gjør ingen tilgangskontroll i bolkoppslag, så bruker av denne metode må ha gjort tilgangskontroll
     */
    fun hentPersonKortBolk(identer: List<String>): Map<String, PdlPersonKort> =
        cacheManager.getCachedOrLoad("pdl-person-kort-bulk", identer.distinct()) { identerUtenCache ->
            identerUtenCache.chunked(50).map { pdlClient.hentPersonKortBolk(it) }.reduce { acc, it -> acc + it }
        }

    fun hentAktørId(ident: String): String = hentAktørIder(ident).gjeldende().ident

    fun hentAktørIder(ident: String): PdlIdenter =
        cacheManager.getValue("pdl-aktørId", ident) {
            pdlClient.hentPersonidenter(ident).aktørIder()
        }

    fun hentGeografiskTilknytning(ident: String): GeografiskTilknytningDto? =
        cacheManager.getNullable("personService_geografiskTilknytning", ident) {
            pdlClient.hentGeografiskTilknytning(ident)
        }

    fun hentVisningsnavnForPerson(personIdent: String): String {
        val person = hentPersonKortBolk(listOf(personIdent))
        return person.visningsnavnFor(personIdent)
    }

    private fun Map<String, PdlPersonKort>.visningsnavnFor(personIdent: String) =
        personIdent
            .let { this[it] }
            ?.navn
            ?.gjeldende()
            ?.visningsnavn() ?: "Mangler navn"

    private fun Map<String, PdlBarn>.identerAndreForeldre(identSøker: String): List<String> =
        this
            .asSequence()
            .flatMap { it.value.forelderBarnRelasjon }
            .filter { it.minRolleForPerson == Familierelasjonsrolle.BARN }
            .mapNotNull { it.relatertPersonsIdent }
            .filter { it != identSøker }
            .distinct()
            .toList()
}
