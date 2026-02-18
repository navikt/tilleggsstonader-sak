package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.kontrakter.felles.Enhet
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.oppfølging.KontrollerOppfølgingResponse

class OppfølgingslisteKall(
    private val testklient: Testklient,
) {
    fun hentAktiveOppfølginger(enhet: Enhet): List<KontrollerOppfølgingResponse> =
        apiRespons.hentAktiveOppfølginger(enhet).expectOkWithBody()

    fun startJobb(enhet: Enhet) = apiRespons.startJobb(enhet).expectOkEmpty()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = OppfølgingslisteApi()

    inner class OppfølgingslisteApi {
        fun hentAktiveOppfølginger(enhet: Enhet) = testklient.get("/api/oppfolging/${enhet.enhetsnr}")

        fun startJobb(enhet: Enhet) = testklient.post("/api/oppfolging/start/${enhet.enhetsnr}")
    }
}
