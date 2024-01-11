package no.nav.tilleggsstonader.sak.journalføring

import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.JournalposterForBrukerRequest
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.kontrakter.søknad.Søknadsskjema
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.SøknadsskjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.vedlegg.VedleggRequest
import org.springframework.stereotype.Service

@Service
class JournalpostService(private val journalpostClient: JournalpostClient, private val personService: PersonService) {
    fun finnJournalposterForBruker(personIdent: String, vedleggRequest: VedleggRequest): List<Journalpost> {
        return journalpostClient.finnJournalposterForBruker(
            JournalposterForBrukerRequest(
                brukerId = Bruker(id = personIdent, type = BrukerIdType.FNR),
                tema = vedleggRequest.tema ?: emptyList(),
                journalposttype = listOfNotNull(vedleggRequest.journalposttype),
                journalstatus = listOfNotNull(vedleggRequest.journalstatus),
                antall = 200,
            ),
        )
    }

    fun hentJournalpost(journalpostId: String): Journalpost {
        return journalpostClient.hentJournalpost(journalpostId)
    }

    fun opprettJournalpost(
        arkiverDokumentRequest: ArkiverDokumentRequest,
        saksbehandler: String? = null,
    ): ArkiverDokumentResponse {
        return journalpostClient.opprettJournalpost(arkiverDokumentRequest, saksbehandler)
    }

    fun oppdaterOgFerdigstillJournalpostMaskinelt(
        journalpost: Journalpost,
        journalførendeEnhet: String,
        fagsak: Fagsak,
    ) = oppdaterOgFerdigstillJournalpost(
        journalpost = journalpost,
        dokumenttitler = null,
        journalførendeEnhet = journalførendeEnhet,
        fagsak = fagsak,
        saksbehandler = null,
    )

    fun oppdaterOgFerdigstillJournalpost(
        journalpost: Journalpost,
        dokumenttitler: Map<String, String>?,
        journalførendeEnhet: String,
        fagsak: Fagsak,
        saksbehandler: String?,
    ) {
        if (journalpost.journalstatus != Journalstatus.JOURNALFOERT) {
            oppdaterJournalpostMedFagsakOgDokumenttitler(
                journalpost = journalpost,
                dokumenttitler = dokumenttitler,
                eksternFagsakId = fagsak.eksternId.id,
                saksbehandler = saksbehandler,
            )
            ferdigstillJournalføring(
                journalpostId = journalpost.journalpostId,
                journalførendeEnhet = journalførendeEnhet,
                saksbehandler = saksbehandler,
            )
        }
    }

    private fun ferdigstillJournalføring(
        journalpostId: String,
        journalførendeEnhet: String,
        saksbehandler: String? = null,
    ) {
        journalpostClient.ferdigstillJournalpost(journalpostId, journalførendeEnhet, saksbehandler)
    }

    private fun oppdaterJournalpostMedFagsakOgDokumenttitler(
        journalpost: Journalpost,
        dokumenttitler: Map<String, String>? = null,
        eksternFagsakId: Long,
        saksbehandler: String? = null,
    ) {
        val oppdatertJournalpost =
            JournalføringHelper.lagOppdaterJournalpostRequest(journalpost, eksternFagsakId, dokumenttitler)
        journalpostClient.oppdaterJournalpost(oppdatertJournalpost, journalpost.journalpostId, saksbehandler)
    }

    fun hentSøknadFraJournalpost(søknadJournalpost: Journalpost): Søknadsskjema<SøknadsskjemaBarnetilsyn> {
        val dokumentinfo = JournalføringHelper.plukkUtOriginaldokument(søknadJournalpost, DokumentBrevkode.BARNETILSYN)
        return journalpostClient.hentSøknadTilsynBarn(søknadJournalpost.journalpostId, dokumentinfo.dokumentInfoId)
    }

    fun finnJournalpostOgPersonIdent(journalpostId: String): Pair<Journalpost, String> {
        val journalpost = hentJournalpost(journalpostId)
        val personIdent = journalpost.bruker?.let {
            when (it.type) {
                BrukerIdType.FNR -> it.id
                BrukerIdType.AKTOERID -> personService.hentPersonIdenter(it.id).gjeldende().ident
                BrukerIdType.ORGNR -> error("Kan ikke hente journalpost=$journalpostId for orgnr")
            }
        } ?: error("Kan ikke hente journalpost=$journalpostId uten bruker")
        return Pair(journalpost, personIdent)
    }

    fun hentDokument(
        journalpost: Journalpost,
        dokumentInfoId: String,
        dokumentVariantformat: Dokumentvariantformat = Dokumentvariantformat.ARKIV,
    ): ByteArray {
        validerDokumentKanHentes(journalpost, dokumentInfoId)
        return journalpostClient.hentDokument(journalpost.journalpostId, dokumentInfoId, dokumentVariantformat)
    }

    private fun validerDokumentKanHentes(
        journalpost: Journalpost,
        dokumentInfoId: String,
    ) {
        val dokument = journalpost.dokumenter?.find { it.dokumentInfoId == dokumentInfoId }
        feilHvis(dokument == null) {
            "Finner ikke dokument med id=$dokumentInfoId for journalpost med id=${journalpost.journalpostId}"
        }
        brukerfeilHvisIkke(
            dokument.dokumentvarianter?.any { it.variantformat == Dokumentvariantformat.ARKIV }
                ?: false,
        ) {
            "Vedlegget er sannsynligvis under arbeid, må åpnes i gosys"
        }
    }
}
