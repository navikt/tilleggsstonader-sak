package no.nav.tilleggsstonader.sak.googlemaps

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.client.bodyWithType
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class GoogleAutocompleteClient(
    @Value("\${google.api-key}") private val apiKey: String,
    builder: RestClient.Builder,
) {
    private val baseUrl = URI("https://places.googleapis.com/v1/places:autocomplete")
    private val restClient = builder.baseUrl(baseUrl.toString()).build()
    private val uri = UriComponentsBuilder.fromUri(baseUrl).encode().toUriString()

    fun hentForslag(request: AutocompleteRequest): AutocompleteResponse? =
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
            .body<AutocompleteResponse>()
}
