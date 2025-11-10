package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.vent.KanTaAvVentDto
import no.nav.tilleggsstonader.sak.behandling.vent.SettPåVentDto
import no.nav.tilleggsstonader.sak.behandling.vent.StatusPåVentDto
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId

class SettPåVentKall(
    private val test: IntegrationTest,
) {
    fun settPaVent(
        behandlingId: BehandlingId,
        settPåVentDto: SettPåVentDto,
    ): StatusPåVentDto = settPaVentResponse(behandlingId, settPåVentDto).expectOkWithBody()

    fun settPaVentResponse(
        behandlingId: BehandlingId,
        settPåVentDto: SettPåVentDto,
    ) = with(test) {
        webTestClient
            .post()
            .uri("/api/sett-pa-vent/$behandlingId")
            .bodyValue(settPåVentDto)
            .medOnBehalfOfToken()
            .exchange()
    }

    fun kanTaAvVent(behandlingId: BehandlingId): KanTaAvVentDto = kanTaAvVentResponse(behandlingId).expectOkWithBody()

    fun kanTaAvVentResponse(behandlingId: BehandlingId) =
        with(test) {
            webTestClient
                .get()
                .uri("/api/sett-pa-vent/$behandlingId/kan-ta-av-vent")
                .medOnBehalfOfToken()
                .exchange()
        }
}
