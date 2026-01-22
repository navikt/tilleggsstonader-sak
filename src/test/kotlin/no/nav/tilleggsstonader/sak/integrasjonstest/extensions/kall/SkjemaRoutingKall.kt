package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.kontrakter.felles.IdentSkjematype
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.migrering.routing.SøknadRoutingResponse

class SkjemaRoutingKall(
    private val testklient: Testklient,
) {
    fun sjekk(identSkjematype: IdentSkjematype): SøknadRoutingResponse = apiRespons.sjekk(identSkjematype).expectOkWithBody()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = SøknadRoutingApi()

    inner class SøknadRoutingApi {
        fun sjekk(identSkjematype: IdentSkjematype) =
            with(testklient.testkontekst) {
                webTestClient
                    .post()
                    .uri("/api/ekstern/skjema-routing")
                    .bodyValue(identSkjematype)
                    .medClientCredentials(
                        clientId = eksternApplikasjon.soknadApi,
                        accessAsApplication = true,
                    ).exchange()
            }
    }
}
