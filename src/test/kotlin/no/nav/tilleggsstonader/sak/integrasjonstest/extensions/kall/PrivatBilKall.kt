package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.IdentRequest
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakDto
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient

class PrivatBilKall(
    private val testklient: Testklient,
) {
    fun hentRammevedtak(dto: IdentRequest) = apiRespons.hentRammevedtak(dto).expectOkWithBody<List<RammevedtakDto>>()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = PrivatBilApi()

    inner class PrivatBilApi {
        fun hentRammevedtak(dto: IdentRequest) =
            with(testklient.testkontekst) {
                restTestClient
                    .post()
                    .uri("/api/ekstern/privat-bil/rammevedtak")
                    .body(dto)
                    .medClientCredentials(eksternApplikasjon.soknadApi, true)
                    .exchange()
            }
    }
}
