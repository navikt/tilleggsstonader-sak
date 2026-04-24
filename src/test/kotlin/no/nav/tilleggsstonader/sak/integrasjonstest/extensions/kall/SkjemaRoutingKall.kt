package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.kontrakter.felles.IdentSkjematype
import no.nav.tilleggsstonader.kontrakter.søknad.felles.SkjemaRoutingResponse
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient

class SkjemaRoutingKall(
    private val testklient: Testklient,
) {
    fun sjekk(identSkjematype: IdentSkjematype): SkjemaRoutingResponse = apiRespons.sjekkV2(identSkjematype).expectOkWithBody()

    fun sjekkV1(identSkjematype: IdentSkjematype): SkjemaRoutingResponse = apiRespons.sjekk(identSkjematype).expectOkWithBody()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = SøknadRoutingApi()

    inner class SøknadRoutingApi {
        fun sjekk(identSkjematype: IdentSkjematype) =
            with(testklient.testkontekst) {
                restTestClient
                    .post()
                    .uri("/api/ekstern/skjema-routing")
                    .body(identSkjematype)
                    .medClientCredentials(
                        clientId = eksternApplikasjon.soknadApi,
                        accessAsApplication = true,
                    ).exchange()
            }

        fun sjekkV2(identSkjematype: IdentSkjematype) =
            with(testklient.testkontekst) {
                restTestClient
                    .post()
                    .uri("/api/ekstern/skjema-routing/v2")
                    .body(identSkjematype)
                    .medClientCredentials(
                        clientId = eksternApplikasjon.soknadApi,
                        accessAsApplication = true,
                    ).exchange()
            }
    }
}
