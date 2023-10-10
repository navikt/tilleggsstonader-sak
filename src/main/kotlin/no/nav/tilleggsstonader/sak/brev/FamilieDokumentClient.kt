package no.nav.tilleggsstonader.soknad.dokument

import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class FamilieDokumentClient(
    @Value("\${clients.familie-dokument.uri}")
    private val dokumentApiURI: URI,
    @Qualifier("utenAuth") restTemplate: RestTemplate,
) :
    AbstractRestClient(restTemplate) {

    private val htmlTilPdfUri =
        UriComponentsBuilder.fromUri(dokumentApiURI).pathSegment("api", "html-til-pdf").toUriString()

    fun genererPdf(html: String): ByteArray =
        postForEntity<ByteArray>(htmlTilPdfUri, html, htmlTilPdfHeaders)

    companion object {

        val htmlTilPdfHeaders = HttpHeaders().apply {
            contentType = MediaType.TEXT_HTML
            accept = listOf(MediaType.APPLICATION_PDF)
        }
    }
}
