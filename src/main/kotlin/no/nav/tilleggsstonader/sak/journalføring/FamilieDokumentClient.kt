package no.nav.tilleggsstonader.sak.journalføring

import no.nav.tilleggsstonader.libs.http.client.postForEntity
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
    @Qualifier("utenAuth") private val restTemplate: RestTemplate,
) {
    private val htmlTilPdfUri =
        UriComponentsBuilder.fromUri(dokumentApiURI).pathSegment("api", "html-til-pdf").toUriString()

    fun genererPdf(html: String): ByteArray = restTemplate.postForEntity<ByteArray>(htmlTilPdfUri, html, htmlTilPdfHeaders)

    companion object {
        val htmlTilPdfHeaders =
            HttpHeaders().apply {
                contentType = MediaType.TEXT_HTML
                accept = listOf(MediaType.APPLICATION_PDF)
            }
    }
}
