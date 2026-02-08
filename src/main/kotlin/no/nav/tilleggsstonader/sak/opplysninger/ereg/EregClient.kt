package no.nav.tilleggsstonader.sak.opplysninger.ereg

import no.nav.tilleggsstonader.libs.http.client.getForEntity
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class EregClient(
    @Value("\${clients.ereg.uri}")
    private val uri: URI,
    @Qualifier("utenAuth") private val restTemplate: RestTemplate,
) {
    fun hentOrganisasjoner(organisasjonsnummer: String): OrganisasjonDto? {
        val uriBuilder =
            UriComponentsBuilder
                .fromUri(eregUri)
                .pathSegment(organisasjonsnummer)
                .build()
                .toUriString()

        return try {
            restTemplate.getForEntity<OrganisasjonDto>(uriBuilder)
        } catch (e: HttpClientErrorException.NotFound) {
            return null
        }
    }

    private val eregUri =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("v1/organisasjon")
            .build()
            .toUri()
}
