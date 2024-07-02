package no.nav.tilleggsstonader.sak.opplysninger.ereg

import java.net.URI
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Component
class EregClient(
    @Value("\${clients.ereg.uri}")
    private val uri: URI,
    @Qualifier("utenAuth") restTemplate: RestTemplate,
) :
    AbstractRestClient(restTemplate) {

    fun hentOrganisasjoner(organisasjonsnumre: List<String>): OrganisasjonsNavnDto? {
        val uriBuilder = UriComponentsBuilder.fromUri(eregUri)
            .pathSegment(organisasjonsnumre.firstOrNull())
            .build()
            .toUriString()

        return try {
            getForEntity<OrganisasjonsNavnDto>(uriBuilder)
        } catch (e: HttpClientErrorException.NotFound) {
            return null
        }
    }

    private val eregUri = UriComponentsBuilder.fromUri(uri)
        .pathSegment("api/v1/organisasjon")
        .build()
        .toUri()
}
