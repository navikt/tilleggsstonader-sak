package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.kontrakter.felles.IdentSkjematype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import no.nav.tilleggsstonader.sak.migrering.routing.SøknadRoutingResponse

class SøknadRoutingKall(
    private val test: IntegrationTest,
) {
    fun skjemaRouting(identSkjematype: IdentSkjematype): SøknadRoutingResponse = skjemaRoutingResponse(identSkjematype).expectOkWithBody()

    fun skjemaRoutingResponse(identSkjematype: IdentSkjematype) =
        with(test) {
            webTestClient
                .post()
                .uri("/api/ekstern/skjema-routing")
                .bodyValue(identSkjematype)
                .medClientCredentials(
                    clientId = EksternApplikasjon.SOKNAD_API.namespaceAppNavn,
                    accessAsApplication = true,
                ).exchange()
        }
}
