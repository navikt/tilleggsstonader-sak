package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.googlemaps.dto.FinnReiseavstandDto
import no.nav.tilleggsstonader.sak.googlemaps.dto.ReisedataDto
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import org.springframework.test.web.reactive.server.WebTestClient

class KartKall(
    private val testklient: Testklient,
) {
    fun hentKjøreavstand(
        fraAdresse: String,
        tilAdresse: String,
    ): ReisedataDto = apiRespons.hentKjoreavstand(fraAdresse, tilAdresse).expectOkWithBody()

    val apiRespons = KartApi()

    inner class KartApi {
        fun hentKjoreavstand(
            fraAdresse: String,
            tilAdresse: String,
        ): WebTestClient.ResponseSpec = testklient.post("/api/kart/kjoreavstand", FinnReiseavstandDto(fraAdresse, tilAdresse))
    }
}
