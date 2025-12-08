package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.SendTilBeslutterRequest
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.StatusTotrinnskontrollDto

class TotrinnskontrollKall(
    private val testklient: Testklient,
) {
    fun sendTilBeslutter(
        behandlingId: BehandlingId,
        dto: SendTilBeslutterRequest = SendTilBeslutterRequest(),
    ): StatusTotrinnskontrollDto = apiRespons.sendTilBeslutter(behandlingId, dto).expectOkWithBody()

    fun beslutteVedtak(
        behandlingId: BehandlingId,
        beslutteVedtakDto: BeslutteVedtakDto,
    ): StatusTotrinnskontrollDto = apiRespons.beslutteVedtak(behandlingId, beslutteVedtakDto).expectOkWithBody()

    fun angreSendTilBeslutter(behandlingId: BehandlingId): StatusTotrinnskontrollDto =
        apiRespons.angreSendTilBeslutter(behandlingId).expectOkWithBody()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = TotrinnskontrollApi()

    inner class TotrinnskontrollApi {
        fun sendTilBeslutter(
            behandlingId: BehandlingId,
            dto: SendTilBeslutterRequest = SendTilBeslutterRequest(),
        ) = testklient.post("/api/totrinnskontroll/$behandlingId/send-til-beslutter", dto)

        fun beslutteVedtak(
            behandlingId: BehandlingId,
            beslutteVedtakDto: BeslutteVedtakDto,
        ) = testklient.post("/api/totrinnskontroll/$behandlingId/beslutte-vedtak", beslutteVedtakDto)

        fun angreSendTilBeslutter(behandlingId: BehandlingId) =
            testklient.post("/api/totrinnskontroll/$behandlingId/angre-send-til-beslutter")
    }
}
