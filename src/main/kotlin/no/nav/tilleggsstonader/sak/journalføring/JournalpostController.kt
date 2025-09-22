package no.nav.tilleggsstonader.sak.journalføring

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.ekstern.journalføring.HåndterSøknadService
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
    private val håndterSøknadService: HåndterSøknadService,
) {
    @GetMapping("/{journalpostId}/dokument-pdf/{dokumentInfoId}", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun hentDokumentSomPdf(
        @PathVariable journalpostId: String,
        @PathVariable dokumentInfoId: String,
    ): ByteArray {
        val (journalpost, personIdent) = journalpostService.finnJournalpostOgPersonIdent(journalpostId)

        tilgangService.validerTilgangTilPerson(personIdent, AuditLoggerEvent.ACCESS)

        return journalpostService.hentDokument(journalpost, dokumentInfoId)
    }

    @GetMapping("/{journalpostId}")
    fun hentJournalpost(
        @PathVariable journalpostId: String,
    ): JournalpostResponse {
        val (journalpost, personIdent) = journalpostService.finnJournalpostOgPersonIdent(journalpostId)
        tilgangService.validerTilgangTilPerson(personIdent, AuditLoggerEvent.ACCESS)
        val valgbareStønadstyperForJournalpost = håndterSøknadService.finnStønadstyperSomKanOpprettesFraJournalpost(journalpost)
        return JournalpostResponse(
            journalpost = journalpost,
            personIdent = personIdent,
            navn = journalpostService.hentBrukersNavn(journalpost, personIdent),
            harStrukturertSøknad = journalpost.harStrukturertSøknad(),
            defaultStønadstype = valgbareStønadstyperForJournalpost.defaultStønadstype,
            valgbareStønadstyper = valgbareStønadstyperForJournalpost.valgbareStønadstyper,
        )
    }

    @PostMapping("/{journalpostId}/fullfor")
    fun fullførJournalpost(
        @PathVariable journalpostId: String,
        @RequestBody journalføringRequest: JournalføringRequest,
    ): String {
        val (journalpost, personIdent) = journalpostService.finnJournalpostOgPersonIdent(journalpostId)
        tilgangService.validerTilgangTilPerson(personIdent, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        journalføringService.fullførJournalpost(journalføringRequest, journalpost)

        return journalpostId
    }
}
