package no.nav.tilleggsstonader.sak.googlemaps.placeDetailsApi

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

@Service
class GooglePlaceDetailsClient(
    @Value("\${google.place-details.uri}") private val baseUrl: URI,
    @Value("\${google.api-key}") private val apiKey: String,
    builder: RestClient.Builder,
) {
    private val restClient = builder.baseUrl(baseUrl.toString()).build()

    fun finnStedDetaljer(stedId: String): PlaceDetailsResponse? =
        restClient
            .get()
            .uri("/places/{placesId}", stedId)
            .headers { headers ->
                headers.apply {
                    add("X-Goog-Api-Key", apiKey)
                    add("X-Goog-FieldMask", "*")
                    add("Content-Type", "application/json")
                }
            }.retrieve()
            .body<PlaceDetailsResponse>()
}
