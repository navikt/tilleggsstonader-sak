package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.IdentRequest
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakDto
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.privatbil.ReisevurderingPrivatBilDto

class PrivatBilKall(
    private val testklient: Testklient,
) {
    fun hentRammevedtak(dto: IdentRequest) = apiRespons.hentRammevedtak(dto).expectOkWithBody<List<RammevedtakDto>>()

    fun hentKjørelisteForBehandling(behandlingId: BehandlingId) =
        apiRespons.hentKjørelisteForBehandling(behandlingId).expectOkWithBody<List<ReisevurderingPrivatBilDto>>()

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

        fun hentKjørelisteForBehandling(behandlingId: BehandlingId) =
            with(testklient.testkontekst) {
                restTestClient
                    .get()
                    .uri("/api/kjoreliste/$behandlingId")
                    .medOnBehalfOfToken()
                    .exchange()
            }
    }
}
