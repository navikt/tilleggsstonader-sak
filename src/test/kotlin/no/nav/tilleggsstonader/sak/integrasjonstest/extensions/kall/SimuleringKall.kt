package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.simulering.dto.SimuleringDto

class SimuleringKall(
    private val test: IntegrationTest,
) {
    fun simulering(behandlingId: BehandlingId): SimuleringDto = simuleringResponse(behandlingId).expectOkWithBody()

    fun simuleringResponse(behandlingId: BehandlingId) =
        with(test) {
            webTestClient
                .get()
                .uri("/api/simulering/$behandlingId")
                .medOnBehalfOfToken()
                .exchange()
        }
}
