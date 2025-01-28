package no.nav.tilleggsstonader.sak.journalføring

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalføringRequest
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalpostResponse
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/journalpost")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class JournalpostController(
    private val journalpostService: JournalpostService,
    private val tilgangService: TilgangService,
    private val journalføringService: JournalføringService,
) {
    @GetMapping("/{journalpostId}/dokument-pdf/{dokumentInfoId}", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun hentDokumentSomPdf(
        @PathVariable journalpostId: String,
        @PathVariable dokumentInfoId: String,
    ): ByteArray {
        val (journalpost, personIdent) = journalpostService.finnJournalpostOgPersonIdent(journalpostId)

        tilgangService.validerTilgangTilPersonMedBarn(personIdent, AuditLoggerEvent.ACCESS)

        return journalpostService.hentDokument(journalpost, dokumentInfoId)
    }

    @GetMapping("/{journalpostId}")
    fun hentJournalPost(
        @PathVariable journalpostId: String,
    ): JournalpostResponse {
        val (journalpost, personIdent) = journalpostService.finnJournalpostOgPersonIdent(journalpostId)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent, AuditLoggerEvent.ACCESS)
        return JournalpostResponse(
            journalpost,
            personIdent,
            journalpostService.hentBrukersNavn(journalpost, personIdent),
            journalpost.harStrukturertSøknad(),
        )
    }

    @PostMapping("/{journalpostId}/fullfor")
    fun fullførJournalpost(
        @PathVariable journalpostId: String,
        @RequestBody journalføringRequest: JournalføringRequest,
    ): String {
        val (journalpost, personIdent) = journalpostService.finnJournalpostOgPersonIdent(journalpostId)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        journalføringService.fullførJournalpost(journalføringRequest, journalpost)

        return journalpostId
    }
}
