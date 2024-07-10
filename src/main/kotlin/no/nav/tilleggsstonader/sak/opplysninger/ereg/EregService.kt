package no.nav.tilleggsstonader.sak.opplysninger.ereg

import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class EregService(private val eregClient: EregClient) {

    @Cacheable("hentOrganisasjon", cacheManager = "shortCache")
    fun hentOrganisasjon(organisasjonsnummer: String): OrganisasjonDto {
        val organisasjon = eregClient.hentOrganisasjoner(organisasjonsnummer)

        return organisasjon ?: throw ApiFeil(
            "Finner ingen organisasjon for søket",
            HttpStatus.BAD_REQUEST,
        )
    }
}
