package no.nav.tilleggsstonader.sak.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingDto
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.behandling.historikk.dto.BehandlingshistorikkDto
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import org.springframework.test.web.reactive.server.expectBody

fun IntegrationTest.hentBehandlingKall(behandlingId: BehandlingId) =
    webTestClient
        .get()
        .uri("/api/behandling/$behandlingId")
        .medOnBehalfOfToken()
        .exchange()

fun IntegrationTest.hentBehandling(behandlingId: BehandlingId) =
    hentBehandlingKall(behandlingId)
        .expectStatus()
        .isOk
        .expectBody<BehandlingDto>()
        .returnResult()
        .responseBody!!

fun IntegrationTest.hentBehandlingMedEksternId(behandlingIdEksternId: Long) =
    webTestClient
        .get()
        .uri("/api/behandling/ekstern/$behandlingIdEksternId")
        .medOnBehalfOfToken()
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<BehandlingId>()
        .returnResult()
        .responseBody!!

fun IntegrationTest.henleggBehandlingKall(
    behandlingId: BehandlingId,
    henlagtDto: HenlagtDto,
) = webTestClient
    .post()
    .uri("/api/behandling/$behandlingId/henlegg")
    .bodyValue(henlagtDto)
    .medOnBehalfOfToken()
    .exchange()

fun IntegrationTest.henleggBehandling(
    behandlingId: BehandlingId,
    henlagtDto: HenlagtDto,
) {
    henleggBehandlingKall(behandlingId, henlagtDto)
        .expectStatus()
        .isNoContent
        .expectBody()
        .isEmpty
}

fun IntegrationTest.hentBehandlingshistorikkKall(behandlingId: BehandlingId) =
    webTestClient
        .get()
        .uri("/api/behandlingshistorikk/$behandlingId")
        .medOnBehalfOfToken()
        .exchange()

fun IntegrationTest.hentBehandlingshistorikk(behandlingId: BehandlingId) =
    hentBehandlingshistorikkKall(behandlingId)
        .expectStatus()
        .isOk
        .expectBody<List<BehandlingshistorikkDto>>()
        .returnResult()
        .responseBody!!
