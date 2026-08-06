package no.nav.tilleggsstonader.sak.opplysninger.ereg

import no.nav.tilleggsstonader.libs.feil.brukerfeil
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class EregService(
    private val eregClient: EregClient,
) {
    @Cacheable("hentOrganisasjon", cacheManager = "shortCache")
    fun hentOrganisasjon(organisasjonsnummer: String): OrganisasjonDto {
        val organisasjon = eregClient.hentOrganisasjoner(organisasjonsnummer)

        return organisasjon ?: brukerfeil(
            "Finner ingen organisasjon for søket",
            HttpStatus.BAD_REQUEST,
        )
    }
}
