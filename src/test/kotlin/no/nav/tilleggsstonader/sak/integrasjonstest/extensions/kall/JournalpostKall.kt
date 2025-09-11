package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalføringRequest
import org.springframework.test.web.reactive.server.expectBody

fun IntegrationTest.fullførJournalpost(
    journalpostId: String,
    request: JournalføringRequest,
) = webTestClient
    .post()
    .uri("/api/journalpost/$journalpostId/fullfor")
    .bodyValue(request)
    .medOnBehalfOfToken()
    .exchange()
    .expectStatus()
    .isOk
    .expectBody<String>()
    .returnResult()
    .responseBody!!
