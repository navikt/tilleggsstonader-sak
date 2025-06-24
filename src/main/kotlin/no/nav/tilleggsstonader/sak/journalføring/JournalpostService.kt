package no.nav.tilleggsstonader.sak.journalføring

import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.tilleggsstonader.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.tilleggsstonader.kontrakter.dokarkiv.BulkOppdaterLogiskVedleggRequest
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.JournalposterForBrukerRequest
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.journalpost.LogiskVedlegg
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.kontrakter.søknad.Skjema
import no.nav.tilleggsstonader.kontrakter.søknad.Søknadsskjema
import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.journalføring.JournalpostDatoUtil.mestRelevanteDato
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.visningsnavn
import no.nav.tilleggsstonader.sak.vedlegg.VedleggRequest
import org.springframework.stereotype.Service

@Service
class JournalpostService(
    private val journalpostClient: JournalpostClient,
    private val personService: PersonService,
) {
    fun finnJournalposterForBruker(
        personIdent: String,
        vedleggRequest: VedleggRequest,
    ): List<Journalpost> =
        journalpostClient.finnJournalposterForBruker(
            JournalposterForBrukerRequest(
                brukerId = Bruker(id = personIdent, type = BrukerIdType.FNR),
                tema = vedleggRequest.tema ?: emptyList(),
                journalposttype = listOfNotNull(vedleggRequest.journalposttype),
                journalstatus = listOfNotNull(vedleggRequest.journalstatus),
                antall = 200,
            ),
        )

    fun hentJournalpost(journalpostId: String): Journalpost = journalpostClient.hentJournalpost(journalpostId)

    fun opprettJournalpost(
        arkiverDokumentRequest: ArkiverDokumentRequest,
        saksbehandler: String? = null,
    ): ArkiverDokumentResponse = journalpostClient.opprettJournalpost(arkiverDokumentRequest, saksbehandler)

    /**
     * Oppdaterer journalposten med fagsak og dokumenttitler, og ferdigstiller journalføringen.
     *
     * @param logiskeVedlegg hvis logiske vedlegg ikke er null, vil de oppdateres
     */
    fun oppdaterOgFerdigstillJournalpost(
        journalpost: Journalpost,
        dokumenttitler: Map<String, String>?,
        logiskeVedlegg: Map<String, List<LogiskVedlegg>>?,
        journalførendeEnhet: String,
        fagsak: Fagsak,
        saksbehandler: String?,
        avsender: AvsenderMottaker? = null,
    ) {
        if (journalpost.journalstatus != Journalstatus.JOURNALFOERT) {
            oppdaterLogiskeVedlegg(journalpost, logiskeVedlegg)
            oppdaterJournalpostMedFagsakOgDokumenttitler(
                journalpost = journalpost,
                dokumenttitler = dokumenttitler,
                eksternFagsakId = fagsak.eksternId.id,
                saksbehandler = saksbehandler,
                avsender = avsender,
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
        avsender: AvsenderMottaker?,
    ) {
        val oppdatertJournalpost =
            JournalføringHelper.lagOppdaterJournalpostRequest(journalpost, eksternFagsakId, dokumenttitler, avsender)
        journalpostClient.oppdaterJournalpost(oppdatertJournalpost, journalpost.journalpostId, saksbehandler)
    }

    private fun oppdaterLogiskeVedlegg(
        journalpost: Journalpost,
        logiskeVedlegg: Map<String, List<LogiskVedlegg>>?,
    ) {
        if (logiskeVedlegg == null) return

        journalpost.dokumenter?.forEach { dokument ->
            val eksisterendeLogiskeVedlegg = dokument.logiskeVedlegg ?: emptyList()
            val logiskeVedleggForDokument = logiskeVedlegg[dokument.dokumentInfoId] ?: emptyList()
            val harIdentiskInnhold =
                eksisterendeLogiskeVedlegg.size == logiskeVedleggForDokument.size &&
                    eksisterendeLogiskeVedlegg.containsAll(
                        logiskeVedleggForDokument,
                    )
            if (!harIdentiskInnhold) {
                journalpostClient.oppdaterLogiskeVedlegg(
                    dokument.dokumentInfoId,
                    BulkOppdaterLogiskVedleggRequest(titler = logiskeVedleggForDokument.map { it.tittel }),
                )
            }
        }
    }

    fun hentSøknadFraJournalpost(
        søknadJournalpost: Journalpost,
        stønadstype: Stønadstype,
    ): Søknadsskjema<out Skjema> {
        val dokumentinfo =
            JournalføringHelper.plukkUtOriginaldokument(søknadJournalpost, stønadstype.tilDokumentBrevkode())
        val data =
            journalpostClient.hentDokument(
                journalpostId = søknadJournalpost.journalpostId,
                dokumentInfoId = dokumentinfo.dokumentInfoId,
                Dokumentvariantformat.ORIGINAL,
            )
        val mottattTidspunkt = mestRelevanteDato(søknadJournalpost) ?: osloNow()
        return SøknadsskjemaUtil.parseSøknadsskjema(stønadstype, data, mottattTidspunkt = mottattTidspunkt)
    }

    fun finnJournalpostOgPersonIdent(journalpostId: String): Pair<Journalpost, String> {
        val journalpost = hentJournalpost(journalpostId)
        val personIdent = hentIdentFraJournalpost(journalpost)
        return Pair(journalpost, personIdent)
    }

    fun hentIdentFraJournalpost(journalpost: Journalpost) =
        journalpost.bruker?.let {
            when (it.type) {
                BrukerIdType.FNR -> it.id
                BrukerIdType.AKTOERID -> personService.hentFolkeregisterIdenter(it.id).gjeldende().ident
                BrukerIdType.ORGNR -> error("Kan ikke hente journalpost=${journalpost.journalpostId} for orgnr")
            }
        } ?: error("Kan ikke hente journalpost=${journalpost.journalpostId} uten bruker")

    fun hentBrukersNavn(
        journalpost: Journalpost,
        personIdent: String,
    ): String =
        journalpost.avsenderMottaker
            ?.takeIf { it.erLikBruker }
            ?.navn
            ?: hentNavnFraPdl(personIdent)

    private fun hentNavnFraPdl(personIdent: String) =
        personService
            .hentPersonKortBolk(listOf(personIdent))
            .getValue(personIdent)
            .navn
            .gjeldende()
            .visningsnavn()

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

private fun Stønadstype.tilDokumentBrevkode(): DokumentBrevkode =
    when (this) {
        Stønadstype.BARNETILSYN -> DokumentBrevkode.BARNETILSYN
        Stønadstype.LÆREMIDLER -> DokumentBrevkode.LÆREMIDLER
        Stønadstype.BOUTGIFTER -> DokumentBrevkode.BOUTGIFTER
        Stønadstype.DAGLIG_REISE_TSO -> DokumentBrevkode.DAGLIG_REISE_TSO
        Stønadstype.DAGLIG_REISE_TSR -> DokumentBrevkode.DAGLIG_REISE_TSR
    }
