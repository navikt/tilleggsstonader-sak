package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.SendTilBeslutterRequest
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.StatusTotrinnskontrollDto

class TotrinnskontrollKall(
    private val test: IntegrationTest,
) {
    fun sendTilBeslutter(
        behandlingId: BehandlingId,
        dto: SendTilBeslutterRequest = SendTilBeslutterRequest(),
    ): StatusTotrinnskontrollDto = sendTilBeslutterResponse(behandlingId, dto).expectOkWithBody()

    fun sendTilBeslutterResponse(
        behandlingId: BehandlingId,
        dto: SendTilBeslutterRequest = SendTilBeslutterRequest(),
    ) = with(test) {
        webTestClient
            .post()
            .uri("/api/totrinnskontroll/$behandlingId/send-til-beslutter")
            .bodyValue(dto)
            .medOnBehalfOfToken()
            .exchange()
    }

    fun beslutteVedtak(
        behandlingId: BehandlingId,
        beslutteVedtakDto: BeslutteVedtakDto,
    ): StatusTotrinnskontrollDto = beslutteVedtakResponse(behandlingId, beslutteVedtakDto).expectOkWithBody()

    fun beslutteVedtakResponse(
        behandlingId: BehandlingId,
        beslutteVedtakDto: BeslutteVedtakDto,
    ) = with(test) {
        webTestClient
            .post()
            .uri("/api/totrinnskontroll/$behandlingId/beslutte-vedtak")
            .bodyValue(beslutteVedtakDto)
            .medOnBehalfOfToken()
            .exchange()
    }
}
