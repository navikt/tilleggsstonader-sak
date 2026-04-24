package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.kontrakter.felles.IdentSkjematype
import no.nav.tilleggsstonader.kontrakter.søknad.felles.SkjemaRoutingResponse
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient

class SkjemaRoutingKall(
    private val testklient: Testklient,
) {
    fun sjekk(identSkjematype: IdentSkjematype): SkjemaRoutingResponse = apiRespons.sjekk(identSkjematype).expectOkWithBody()

    // Gir tilgang til "rå"-endepunktet slik at tester kan skrive egne assertions på responsen.
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
    }
}
