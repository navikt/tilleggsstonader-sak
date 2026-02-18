package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.kontrakter.felles.Enhet
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.oppfølging.KontrollerOppfølgingResponse

class OppfølgingslisteKall(
    private val testklient: Testklient,
) {
    fun hentAktiveOppfølginger(enhet: Enhet): List<KontrollerOppfølgingResponse> =
        apiRespons.hentAktiveOppfølginger(enhet).expectOkWithBody()

    fun startJobb() = apiRespons.startJobb().expectOkEmpty()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = OppfølgingslisteApi()

    inner class OppfølgingslisteApi {
        fun hentAktiveOppfølginger(enhet: Enhet) = testklient.get("/api/oppfolging/$enhet")

        fun startJobb() = testklient.post("/api/oppfolging/start")
    }
}
