package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalføringRequest
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalpostResponse

class JournalpostKall(
    private val testklient: Testklient,
) {
    fun fullfor(
        journalpostId: String,
        request: JournalføringRequest,
    ): String = apiRespons.fullfor(journalpostId, request).expectOkWithBody()

    fun journalpost(journalpostId: String): JournalpostResponse = apiRespons.journalpost(journalpostId).expectOkWithBody()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = JournalpostApi()

    inner class JournalpostApi {
        fun fullfor(
            journalpostId: String,
            request: JournalføringRequest,
        ) = testklient.post("/api/journalpost/$journalpostId/fullfor", request)

        fun journalpost(journalpostId: String) = testklient.get("/api/journalpost/$journalpostId")
    }
}
