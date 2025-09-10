package no.nav.tilleggsstonader.sak.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.simulering.dto.SimuleringDto
import org.springframework.test.web.reactive.server.expectBody

fun IntegrationTest.simulerForBehandling(behandlingId: BehandlingId) =
    webTestClient
        .get()
        .uri("/api/simulering/$behandlingId")
        .medOnBehalfOfToken()
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<SimuleringDto>()
        .returnResult()
        .responseBody!!
