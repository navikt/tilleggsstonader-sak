package no.nav.tilleggsstonader.sak.opplysninger.fullmakt

import no.nav.tilleggsstonader.kontrakter.felles.IdentRequest
import no.nav.tilleggsstonader.kontrakter.fullmakt.FullmektigDto
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class FullmaktClient(
    @Value("\${clients.integrasjoner.uri}") private val baseUrl: URI,
    @Qualifier("azureClientCredential") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {
    private val uriFullmektige =
        UriComponentsBuilder
            .fromUri(baseUrl)
            .pathSegment("api", "fullmakt", "fullmektige")
            .encode()
            .toUriString()

    fun hentFullmektige(fullmaktsgiversIdent: String): List<FullmektigDto> =
        postForEntity(
            uri = uriFullmektige,
            payload = IdentRequest(fullmaktsgiversIdent),
        )
}
