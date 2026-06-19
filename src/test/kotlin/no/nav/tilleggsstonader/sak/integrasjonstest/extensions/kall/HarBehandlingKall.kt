package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.kontrakter.felles.IdentSkjematype
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.sak.ekstern.stønad.HarBehandlingRequest
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient

class HarBehandlingKall(
    private val testklient: Testklient,
) {
    fun harBehandling(identStønadstype: IdentStønadstype): Boolean =
        apiRespons
            .harBehandling(
                HarBehandlingRequest(identStønadstype.ident, stønadstype = identStønadstype.stønadstype),
            ).expectOkWithBody()

    fun harBehandling(identSkjematype: IdentSkjematype): Boolean =
        apiRespons.harBehandling(HarBehandlingRequest(identSkjematype.ident, skjematype = identSkjematype.skjematype)).expectOkWithBody()

    // Gir tilgang til "rå"-endepunktet slik at tester kan skrive egne assertions på responsen.
    val apiRespons = HarBehandlingApi()

    inner class HarBehandlingApi {
        fun harBehandling(body: Any) =
            with(testklient.testkontekst) {
                restTestClient
                    .post()
                    .uri("/api/ekstern/har-behandling")
                    .body(body)
                    .medClientCredentials(
                        clientId = eksternApplikasjon.soknadApi,
                        accessAsApplication = true,
                    ).exchange()
            }

        fun harBehandlingMedFeilKlient(body: Any) =
            with(testklient.testkontekst) {
                restTestClient
                    .post()
                    .uri("/api/ekstern/har-behandling")
                    .body(body)
                    .medClientCredentials(
                        clientId = eksternApplikasjon.arena,
                        accessAsApplication = true,
                    ).exchange()
            }
    }
}
