package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingDto
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.behandling.historikk.dto.BehandlingshistorikkDto
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import org.springframework.test.web.reactive.server.expectBody

class BehandlingKall(
    private val test: IntegrationTest,
) {
    fun hentBehandling(behandlingId: BehandlingId): BehandlingDto = hentBehandlingResponse(behandlingId).expectOkWithBody<BehandlingDto>()

    fun hentBehandlingResponse(behandlingId: BehandlingId) =
        with(test) {
            webTestClient
                .get()
                .uri("/api/behandling/$behandlingId")
                .medOnBehalfOfToken()
                .exchange()
        }

    fun ekstern(behandlingIdEksternId: Long) = eksternResponse(behandlingIdEksternId).expectOkWithBody<BehandlingId>()

    fun eksternResponse(behandlingIdEksternId: Long) =
        with(test) {
            webTestClient
                .get()
                .uri("/api/behandling/ekstern/$behandlingIdEksternId")
                .medOnBehalfOfToken()
                .exchange()
        }

    fun henlegg(
        behandlingId: BehandlingId,
        henlagtDto: HenlagtDto,
    ) = henleggResponse(behandlingId, henlagtDto)
        .expectStatus()
        .isNoContent()
        .expectBody()
        .isEmpty()

    fun henleggResponse(
        behandlingId: BehandlingId,
        henlagtDto: HenlagtDto,
    ) = with(test) {
        webTestClient
            .post()
            .uri("/api/behandling/$behandlingId/henlegg")
            .bodyValue(henlagtDto)
            .medOnBehalfOfToken()
            .exchange()
    }

    fun behandlingshistorikkResponse(behandlingId: BehandlingId) =
        with(test) {
            webTestClient
                .get()
                .uri("/api/behandlingshistorikk/$behandlingId")
                .medOnBehalfOfToken()
                .exchange()
        }

    fun behandlingshistorikk(behandlingId: BehandlingId): List<BehandlingshistorikkDto> =
        behandlingshistorikkResponse(behandlingId)
            .expectStatus()
            .isOk
            .expectBody<List<BehandlingshistorikkDto>>()
            .returnResult()
            .responseBody!!
}
