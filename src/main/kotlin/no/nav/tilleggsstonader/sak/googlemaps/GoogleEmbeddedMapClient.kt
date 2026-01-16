package no.nav.tilleggsstonader.sak.googlemaps

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.servlet.view.RedirectView
import org.springframework.web.util.UriComponentsBuilder

@Service
class GoogleEmbeddedMapClient(
    @Value($$"${google.embedded-map.uri}") private val baseUrl: String,
    @Value($$"${google.api-key}") private val apiKey: String,
) {
    fun embeddedMapRedirect(
        origin: String,
        destination: String,
        mode: String,
    ): RedirectView {
        val uri =
            UriComponentsBuilder
                .fromUriString(baseUrl)
                .queryParam("key", apiKey)
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("mode", mode)
                .build()
                .toUri()

        return RedirectView(uri.toString())
    }
}
