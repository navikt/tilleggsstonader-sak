package no.nav.tilleggsstonader.sak.googlemaps

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.client.bodyWithType
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class GoogleRoutesClient(
    @Value("https://routes.googleapis.com/directions/v2:computeRoutes")
    private val baseUrl: URI,
    @Value($$"${GOOGLE_MAPS_API_KEY}") private val apiKey: String,
    builder: RestClient.Builder,
) {
    private val restClient = builder.baseUrl(baseUrl.toString()).build()
    private val uri = UriComponentsBuilder.fromUri(baseUrl).encode().toUriString()

    fun hentRuter(request: RuteRequest): RuteDto =
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
            .body<RuteDto>()!!
}
