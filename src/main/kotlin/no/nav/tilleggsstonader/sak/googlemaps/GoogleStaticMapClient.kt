package no.nav.tilleggsstonader.sak.googlemaps

import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class GoogleStaticMapClient(
    builder: RestClient.Builder,
) {
    private val apiKey = "nøkkel" // TODO fjern

    private val baseUrl =
        URI(
            "https://maps.googleapis.com/maps/api/staticmap",
        )

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
