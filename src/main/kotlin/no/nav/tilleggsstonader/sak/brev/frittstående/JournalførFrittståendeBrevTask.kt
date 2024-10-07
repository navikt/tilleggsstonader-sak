package no.nav.tilleggsstonader.sak.brev.frittstående

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokument
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokumenttype
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Filtype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakereFrittståendeBrevService
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerUtil.tilAvsenderMottaker
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerFrittståendeBrev
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.journalføring.ArkiverDokumentConflictException
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = JournalførFrittståendeBrevTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 31L,
    beskrivelse = "Journalfører vedtaksbrev",
)
class JournalførFrittståendeBrevTask(
    private val taskService: TaskService,
    private val fagsakService: FagsakService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val journalpostService: JournalpostService,
    private val frittståendeBrevService: FrittståendeBrevService,
    private val brevmottakereFrittståendeBrevService: BrevmottakereFrittståendeBrevService,
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val mottakerId = UUID.fromString(task.payload)

        val brevmottaker = brevmottakereFrittståendeBrevService.hentBrevmottakere(mottakerId)
        val brevId = brevmottaker.brevId ?: error("Mangler brevId på brevmottaker=$mottakerId")
        val brev = frittståendeBrevService.hentFrittståendeBrev(brevId)

        val arkiverDokumentResponse = journalførBrev(brev, brevmottaker)

        val journalpostId = arkiverDokumentResponse.journalpostId
        brevmottakereFrittståendeBrevService.oppdaterBrevmottaker(brevmottaker.copy(journalpostId = journalpostId))
        taskService.save(DistribuerFrittståendeBrevTask.opprettTask(brev.fagsakId, journalpostId, mottakerId))
    }

    private fun journalførBrev(
        brev: FrittståendeBrev,
        brevmottaker: BrevmottakerFrittståendeBrev,
    ): ArkiverDokumentResponse {
        val fagsak = fagsakService.hentFagsak(brev.fagsakId)
        val dokument = Dokument(
            dokument = brev.pdf.bytes,
            filtype = Filtype.PDFA,
            dokumenttype = utledDokumenttype(fagsak.stønadstype),
            tittel = brev.tittel,
        )

        val eksternReferanseId = "frittstående-brev-${brevmottaker.id}"
        val arkiverDokumentRequest = ArkiverDokumentRequest(
            fnr = fagsak.hentAktivIdent(),
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(dokument),
            fagsakId = fagsak.eksternId.id.toString(),
            journalførendeEnhet = arbeidsfordelingService.hentNavEnhet(fagsak.hentAktivIdent())?.enhetNr
                ?: error("Fant ikke arbeidsfordelingsenhet"),
            eksternReferanseId = eksternReferanseId,
            avsenderMottaker = brevmottaker.mottaker.tilAvsenderMottaker(),
        )

        return opprettJournalpost(arkiverDokumentRequest, brevmottaker, brev)
    }

    private fun opprettJournalpost(
        arkviverDokumentRequest: ArkiverDokumentRequest,
        brevmottaker: BrevmottakerFrittståendeBrev,
        brev: FrittståendeBrev,
    ): ArkiverDokumentResponse {
        val response = try {
            journalpostService.opprettJournalpost(arkviverDokumentRequest)
        } catch (e: ArkiverDokumentConflictException) {
            logger.warn(
                "Konflikt ved arkivering av dokument. Brevet=${brev.id} og mottaker=${brevmottaker.id}" +
                    " har allerede blitt journalført med eksternReferanseId=${arkviverDokumentRequest.eksternReferanseId}",
            )
            e.response
        }
        feilHvisIkke(response.ferdigstilt) {
            "Journalposten ble ikke ferdigstilt og kan derfor ikke distribueres"
        }
        return response
    }

    private fun utledDokumenttype(stønadstype: Stønadstype) =
        when (stønadstype) {
            Stønadstype.BARNETILSYN -> Dokumenttype.BARNETILSYN_FRITTSTÅENDE_BREV
            Stønadstype.LÆREMIDLER -> Dokumenttype.LÆREMIDLER_FRITTSTÅENDE_BREV
            else -> error("Utledning av dokumenttype er ikke implementert for $stønadstype")
        }

    companion object {

        fun opprettTask(fagsakId: FagsakId, brevId: UUID, mottakerId: UUID): Task =
            Task(
                type = TYPE,
                payload = mottakerId.toString(),
                properties = Properties().apply {
                    setProperty("fagsakId", fagsakId.toString())
                    setProperty("brevId", brevId.toString())
                    setProperty("mottakerId", mottakerId.toString())
                },
            )

        const val TYPE = "journalførFrittståendeBrev"
    }
}
