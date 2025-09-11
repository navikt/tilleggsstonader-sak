package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.vent.KanTaAvVentDto
import no.nav.tilleggsstonader.sak.behandling.vent.SettPåVentDto
import no.nav.tilleggsstonader.sak.behandling.vent.StatusPåVentDto
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import org.springframework.test.web.reactive.server.expectBody

fun IntegrationTest.settPåVentKall(
    behandlingId: BehandlingId,
    settPåVentDto: SettPåVentDto,
) = webTestClient
    .post()
    .uri("/api/sett-pa-vent/$behandlingId")
    .bodyValue(settPåVentDto)
    .medOnBehalfOfToken()
    .exchange()

fun IntegrationTest.settPåVent(
    behandlingId: BehandlingId,
    settPåVentDto: SettPåVentDto,
) = settPåVentKall(behandlingId, settPåVentDto)
    .expectStatus()
    .isOk
    .expectBody<StatusPåVentDto>()
    .returnResult()
    .responseBody!!

fun IntegrationTest.hentKanTaAvVent(behandlingId: BehandlingId) =
    webTestClient
        .get()
        .uri("/api/sett-pa-vent/$behandlingId/kan-ta-av-vent")
        .medOnBehalfOfToken()
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<KanTaAvVentDto>()
        .returnResult()
        .responseBody!!
