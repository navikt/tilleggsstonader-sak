package no.nav.tilleggsstonader.sak.opplysninger.ereg

import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class EregClient(
    @Value("\${clients.ereg.uri}")
    private val eregUrl: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {

    fun hentOrganisasjoner(organisasjonsnumre: List<String>): OrganisasjonsNavnDto? {
        val uriBuilder = UriComponentsBuilder.fromUri(eregUrl)
            .pathSegment("api/v1/organisasjon")
            .pathSegment(organisasjonsnumre.firstOrNull())
        return try {
            getForEntity<OrganisasjonsNavnDto>(uriBuilder.build().toUri().toString())
        } catch (e: HttpClientErrorException.NotFound) {
            return null
        }
    }
}
