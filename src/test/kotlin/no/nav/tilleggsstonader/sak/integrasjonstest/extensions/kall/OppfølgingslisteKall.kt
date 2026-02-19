package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.oppfølging.KontrollerOppfølgingResponse

class OppfølgingslisteKall(
    private val testklient: Testklient,
) {
    fun hentAktiveOppfølginger(tema: Tema): List<KontrollerOppfølgingResponse> = apiRespons.hentAktiveOppfølginger(tema).expectOkWithBody()

    fun startJobb(tema: Tema) = apiRespons.startJobb(tema).expectOkEmpty()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = OppfølgingslisteApi()

    inner class OppfølgingslisteApi {
        fun hentAktiveOppfølginger(tema: Tema) = testklient.get("/api/oppfolging/$tema")

        fun startJobb(tema: Tema) = testklient.post("/api/oppfolging/start/$tema")
    }
}
