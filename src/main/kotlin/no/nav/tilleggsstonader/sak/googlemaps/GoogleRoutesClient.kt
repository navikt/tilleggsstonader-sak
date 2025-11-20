package no.nav.tilleggsstonader.sak.googlemaps

import ReisedataDto
import RuteDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.client.bodyWithType
import org.springframework.web.util.UriComponentsBuilder
import tilDto
import java.net.URI

@Service
class GoogleRoutesClient(
    @Value("\${google.uri}")
    private val baseUrl: URI,
    @Value("\${google.api-key}") private val apiKey: String,
    builder: RestClient.Builder,
) {
    private val restClient = builder.baseUrl(baseUrl.toString()).build()
    private val uri = UriComponentsBuilder.fromUri(baseUrl).encode().toUriString()

    fun hentRuter(request: RuteRequest): RuteResponse? =
        restClient
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
            .body<RuteResponse>()

    fun hentRuterV2(request: RuteRequest): ReisedataDto? =
        restClient
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
            .body<RuteResponse>()
            ?.tilDto()
}
