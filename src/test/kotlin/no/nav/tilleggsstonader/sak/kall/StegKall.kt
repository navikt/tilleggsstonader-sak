package no.nav.tilleggsstonader.sak.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegController
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId

fun IntegrationTest.resetSteg(
    behandlingId: BehandlingId,
    resetStegRequest: StegController.ResetStegRequest,
) {
    webTestClient
        .post()
        .uri("/api/steg/behandling/$behandlingId/reset")
        .bodyValue(resetStegRequest)
        .medOnBehalfOfToken()
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .isEmpty
}
