package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegController
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId

class StegKall(
    private val test: IntegrationTest,
) {
    fun reset(
        behandlingId: BehandlingId,
        resetStegRequest: StegController.ResetStegRequest,
    ) = resetResponse(behandlingId, resetStegRequest).expectOkEmpty()

    fun resetResponse(
        behandlingId: BehandlingId,
        resetStegRequest: StegController.ResetStegRequest,
    ) = with(test) {
        webTestClient
            .post()
            .uri("/api/steg/behandling/$behandlingId/reset")
            .bodyValue(resetStegRequest)
            .medOnBehalfOfToken()
            .exchange()
    }

    fun ferdigstill(
        behandlingId: BehandlingId,
        dto: StegController.FerdigstillStegRequest,
    ) = ferdigstillResponse(behandlingId, dto).expectOkWithBody<BehandlingId>()

    fun ferdigstillResponse(
        behandlingId: BehandlingId,
        dto: StegController.FerdigstillStegRequest,
    ) = with(test) {
        webTestClient
            .post()
            .uri("api/steg/behandling/$behandlingId/ferdigstill")
            .bodyValue(dto)
            .medOnBehalfOfToken()
            .exchange()
    }
}
