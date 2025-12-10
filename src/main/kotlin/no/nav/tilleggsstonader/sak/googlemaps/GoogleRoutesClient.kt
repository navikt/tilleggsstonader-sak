package no.nav.tilleggsstonader.sak.googlemaps

import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.opplysninger.pdl.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.client.bodyWithType
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class GoogleRoutesClient(
    @Value("\${google.routes.uri}") private val baseUrl: URI,
    @Value("\${google.api-key}") private val apiKey: String,
    builder: RestClient.Builder,
) {
    private val restClient = builder.baseUrl(baseUrl.toString()).build()
    private val uri = UriComponentsBuilder.fromUri(baseUrl).encode().toUriString()

    fun hentRuter(request: RuteRequest): RuteResponse? {
        secureLogger.info("Reise fra ${request.origin} til ${request.destination}")
        return restClient
            .post()
            .uri(uri)
            .headers { headers ->
                headers.apply {
                    add("X-Goog-Api-Key", apiKey)
                    add("X-Goog-FieldMask", "*")
                    add("Content-Type", "application/json")
                }
            }.bodyWithType(request)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, clientErrorHandler)
            .body<RuteResponse>()
    }

    private val clientErrorHandler: (HttpRequest, ClientHttpResponse) -> Unit = { _, response ->
        val body = String(response.body.readAllBytes())
        if (body.contains("Address not found")) {
            logger.warn(body)
            brukerfeil("Kunne ikke finne adressen")
        } else {
            logger.error(body)
            feil("Kunne ikke finne ruteforslag")
        }
    }
}
