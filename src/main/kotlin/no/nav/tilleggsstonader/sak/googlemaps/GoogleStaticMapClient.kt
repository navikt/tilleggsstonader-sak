package no.nav.tilleggsstonader.sak.googlemaps

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class GoogleStaticMapClient(
    @Value("\${google.static-map.uri}") private val baseUrl: URI,
    @Value("\${google.api-key}") private val apiKey: String,
    builder: RestClient.Builder,
) {
    private val restClient = builder.baseUrl(baseUrl.toString()).build()

    fun hentStaticMap(polyline: String): ByteArray? {
        val uriString = "https://maps.googleapis.com/maps/api/staticmap?size=600x300&path=enc:$polyline&key=$apiKey"

        val uri =
            UriComponentsBuilder
                .fromUriString(uriString)
                .build(false) // Kan ikke encode pga av polyline fra GoogleMaps som ikke kan endre noen karakterer
                .toUri()

        return restClient
            .get()
            .uri(uri)
            .retrieve()
            .body(ByteArray::class.java)
    }
}
