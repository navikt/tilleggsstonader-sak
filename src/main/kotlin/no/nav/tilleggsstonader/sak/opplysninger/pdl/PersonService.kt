package no.nav.tilleggsstonader.sak.opplysninger.pdl

import no.nav.tilleggsstonader.sak.infrastruktur.config.getCachedOrLoad
import no.nav.tilleggsstonader.sak.opplysninger.dto.SøkerMedBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Familierelasjonsrolle
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlAnnenForelder
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonKort
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlSøker
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class PersonService(
    private val pdlClient: PdlClient,
    @Qualifier("shortCache")
    private val cacheManager: CacheManager,
) {
    fun hentSøker(ident: String): PdlSøker {
        return pdlClient.hentSøker(ident)
    }

    fun hentPersonMedBarn(ident: String): SøkerMedBarn {
        val søker = hentSøker(ident)
        val barnIdentifikatorer =
            søker.forelderBarnRelasjon.filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
                .mapNotNull { it.relatertPersonsIdent }
        return SøkerMedBarn(ident, søker, hentPersonForelderBarnRelasjon(barnIdentifikatorer))
    }

    fun hentPersonForelderBarnRelasjon(barnIdentifikatorer: List<String>) =
        pdlClient.hentPersonForelderBarnRelasjon(barnIdentifikatorer)

    fun hentAndreForeldre(personIdenter: List<String>): Map<String, PdlAnnenForelder> {
        return pdlClient.hentAndreForeldre(personIdenter)
    }

    @Cacheable("personidenter")
    fun hentPersonIdenter(ident: String): PdlIdenter =
        pdlClient.hentPersonidenter(ident = ident)

    fun hentIdenterBolk(identer: List<String>): Map<String, PdlIdent> =
        pdlClient.hentIdenterBolk(identer)

    /**
     * PDL gjør ingen tilgangskontroll i bolkoppslag, så bruker av denne metode må ha gjort tilgangskontroll
     */
    fun hentPersonKortBolk(identer: List<String>): Map<String, PdlPersonKort> {
        return cacheManager.getCachedOrLoad("pdl-person-kort-bulk", identer.distinct()) { identerUtenCache ->
            identerUtenCache.chunked(50).map { pdlClient.hentPersonKortBolk(it) }.reduce { acc, it -> acc + it }
        }
    }

    fun hentAktørIder(ident: String): PdlIdenter = pdlClient.hentAktørIder(ident)
}
