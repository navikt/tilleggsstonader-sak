package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId

// Må kalles med utvikler-rettighet
class GjenopprettOppgaveKall(
    private val test: IntegrationTest,
) {
    fun gjenopprett(behandlingId: BehandlingId) = gjenopprettResponse(behandlingId).expectOkEmpty()

    fun gjenopprettResponse(behandlingId: BehandlingId) =
        with(test) {
            webTestClient
                .post()
                .uri("/api/forvaltning/oppgave/gjenopprett/$behandlingId")
                .medOnBehalfOfToken()
                .exchange()
        }
}
