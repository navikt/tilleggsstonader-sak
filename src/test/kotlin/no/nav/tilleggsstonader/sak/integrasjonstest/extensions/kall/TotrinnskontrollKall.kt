package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.SendTilBeslutterRequest

class TotrinnskontrollKall(
    private val test: IntegrationTest,
) {
    fun sendTilBeslutter(
        behandlingId: BehandlingId,
        dto: SendTilBeslutterRequest = SendTilBeslutterRequest(),
    ) {
        with(test) {
            webTestClient
                .post()
                .uri("/api/totrinnskontroll/$behandlingId/send-til-beslutter")
                .bodyValue(dto)
                .medOnBehalfOfToken()
                .exchange()
                .expectStatus()
                .isOk
        }
    }

    fun beslutteVedtak(
        behandlingId: BehandlingId,
        beslutteVedtakDto: BeslutteVedtakDto,
    ) {
        with(test) {
            webTestClient
                .post()
                .uri("/api/totrinnskontroll/$behandlingId/beslutte-vedtak")
                .bodyValue(beslutteVedtakDto)
                .medOnBehalfOfToken()
                .exchange()
                .expectStatus()
                .isOk
        }
    }
}
