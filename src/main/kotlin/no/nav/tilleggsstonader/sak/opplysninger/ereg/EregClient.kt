package no.nav.tilleggsstonader.sak.opplysninger.ereg

import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
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
    @Qualifier("utenAuth") restTemplate: RestTemplate,
) :
    AbstractRestClient(restTemplate) {

    fun hentOrganisasjoner(organisasjonsnummer: String): OrganisasjonDto? {
        val uriBuilder = UriComponentsBuilder.fromUri(eregUri)
            .pathSegment(organisasjonsnummer)
            .build()
            .toUriString()

        return try {
            getForEntity<OrganisasjonDto>(uriBuilder)
        } catch (e: HttpClientErrorException.NotFound) {
            return null
        }
    }

    private val eregUri = UriComponentsBuilder.fromUri(uri)
        .pathSegment("v1/organisasjon")
        .build()
        .toUri()
}
