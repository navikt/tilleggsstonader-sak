package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalføringRequest
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalpostResponse

class JournalpostKall(
    private val test: IntegrationTest,
) {
    fun fullfor(
        journalpostId: String,
        request: JournalføringRequest,
    ): String = fullforResponse(journalpostId, request).expectOkWithBody()

    fun fullforResponse(
        journalpostId: String,
        request: JournalføringRequest,
    ) = with(test) {
        webTestClient
            .post()
            .uri("/api/journalpost/$journalpostId/fullfor")
            .bodyValue(request)
            .medOnBehalfOfToken()
            .exchange()
    }

    fun journalpost(journalpostId: String): JournalpostResponse = journalpostResponse(journalpostId).expectOkWithBody()

    fun journalpostResponse(journalpostId: String) =
        with(test) {
            webTestClient
                .get()
                .uri("/api/journalpost/$journalpostId")
                .medOnBehalfOfToken()
                .exchange()
        }
}
