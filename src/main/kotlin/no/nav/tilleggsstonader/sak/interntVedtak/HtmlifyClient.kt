package no.nav.tilleggsstonader.sak.interntVedtak

import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class HtmlifyClient(
    @Value("\${clients.htmlify.uri}")
    private val uri: URI,
    @Qualifier("utenAuth") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {

    fun generateHtml(interntVedtak: InterntVedtak): String {
        return postForEntity<String>(
            UriComponentsBuilder.fromUri(uri).pathSegment("api", "internt-vedtak").toUriString(),
            interntVedtak,
            httpHeaders = HttpHeaders(),
        )
    }

    fun generateHtml(interntVedtak: InterntVedtakV2): String {
        return postForEntity<String>(
            UriComponentsBuilder.fromUri(uri).pathSegment("api", "internt-vedtak-v2").toUriString(),
            interntVedtak,
            httpHeaders = HttpHeaders(),
        )
    }
}
