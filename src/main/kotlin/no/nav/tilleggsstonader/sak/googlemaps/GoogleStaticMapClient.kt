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

    fun hentStaticMap(statiskKartRequest: StatiskKartRequest): ByteArray? {
        // URL-er kan maks være 2048 tegn. En lang polyline kan derfor gjøre kart-requesten ugyldig.
        // Viser ikke polylinen hvis den blir for lang.
        val path = if (statiskKartRequest.polyline.length < 1900) "&path=enc:${statiskKartRequest.polyline}" else ""
        val startMarkør =
            "&markers=color:green|${statiskKartRequest.startLokasjon.lat},${statiskKartRequest.startLokasjon.lng}"
        val sluttMarkør =
            "&markers=color:red|${statiskKartRequest.sluttLokasjon.lat},${statiskKartRequest.sluttLokasjon.lng}"

        val uriString =
            "$baseUrl?size=900x500$path$startMarkør$sluttMarkør&key=$apiKey"

        val uri =
            UriComponentsBuilder
                .fromUriString(uriString)
                .build(false) // Kan ikke encode pga av polyline fra GoogleMaps som ikke klarer å tolke encoded karakterer
                .toUri()

        return restClient
            .get()
            .uri(uri)
            .retrieve()
            .body(ByteArray::class.java)
    }
}
