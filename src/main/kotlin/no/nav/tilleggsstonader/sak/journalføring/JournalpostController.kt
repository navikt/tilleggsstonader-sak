package no.nav.tilleggsstonader.sak.journalføring

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/journalpost")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class JournalpostController(
    private val journalpostService: JournalpostService,
    private val personService: PersonService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("/{journalpostId}/dokument-pdf/{dokumentInfoId}", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun hentDokumentSomPdf(@PathVariable journalpostId: String, @PathVariable dokumentInfoId: String): ByteArray {
        val (journalpost, personIdent) = finnJournalpostOgPersonIdent(journalpostId)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent, AuditLoggerEvent.ACCESS)
        validerDokumentKanHentes(journalpost, dokumentInfoId, journalpostId)
        return journalpostService.hentDokument(journalpostId, dokumentInfoId)
    }

    private fun validerDokumentKanHentes(
        journalpost: Journalpost,
        dokumentInfoId: String,
        journalpostId: String,
    ) {
        val dokument = journalpost.dokumenter?.find { it.dokumentInfoId == dokumentInfoId }
        feilHvis(dokument == null) {
            "Finner ikke dokument med $dokumentInfoId for journalpost=$journalpostId"
        }
        brukerfeilHvisIkke(
            dokument.dokumentvarianter?.any { it.variantformat == Dokumentvariantformat.ARKIV }
                ?: false,
        ) {
            "Vedlegget er sannsynligvis under arbeid, må åpnes i gosys"
        }
    }

    private fun finnJournalpostOgPersonIdent(journalpostId: String): Pair<Journalpost, String> {
        val journalpost = journalpostService.hentJournalpost(journalpostId)
        val personIdent = journalpost.bruker?.let {
            when (it.type) {
                BrukerIdType.FNR -> it.id
                BrukerIdType.AKTOERID -> personService.hentPersonIdenter(it.id).gjeldende().ident
                BrukerIdType.ORGNR -> error("Kan ikke hente journalpost=$journalpostId for orgnr")
            }
        } ?: error("Kan ikke hente journalpost=$journalpostId uten bruker")
        return Pair(journalpost, personIdent)
    }
}
