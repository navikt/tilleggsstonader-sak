package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakDto
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient

class PrivatBilKall(
    private val testklient: Testklient,
) {
    fun hentRammevedtak(ident: String) = apiRespons.hentRammevedtak(ident).expectOkWithBody<List<RammevedtakDto>>()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = PrivatBilApi()

    inner class PrivatBilApi {
        fun hentRammevedtak(ident: String) =
            with(testklient.testkontekst) {
                restTestClient
                    .get()
                    .uri("/api/ekstern/privat-bil/rammevedtak")
                    .medTokenXToken(eksternApplikasjon.soknadApi, ident)
                    .exchange()
            }
    }
}
