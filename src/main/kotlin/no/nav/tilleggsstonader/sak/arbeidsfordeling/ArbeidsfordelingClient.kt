package no.nav.tilleggsstonader.sak.arbeidsfordeling

import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class ArbeidsfordelingClient(
    @Value("\${clients.norg2.uri}")
    private val uri: URI,
    @Qualifier("utenAuth") restTemplate: RestTemplate,
) :
    AbstractRestClient(restTemplate) {

    fun finnArberidsfordelingsenhet(arbeidsfordelingskriterie: ArbeidsfordelingKriterie): List<Arbeidsfordelingsenhet> {
        return postForEntity(arbeidsfordelingUri, arbeidsfordelingskriterie)
    }

    private val arbeidsfordelingUri = UriComponentsBuilder.fromUri(uri)
        .pathSegment("api/v1/arbeidsfordeling")
        .build()
        .toUriString()
}
