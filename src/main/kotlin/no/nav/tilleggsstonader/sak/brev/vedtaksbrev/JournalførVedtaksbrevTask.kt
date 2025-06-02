package no.nav.tilleggsstonader.sak.brev.vedtaksbrev

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokument
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Filtype
import no.nav.tilleggsstonader.kontrakter.dokarkiv.dokumenttyper
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerUtil.tilAvsenderMottaker
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.journalføring.ArkiverDokumentConflictException
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = JournalførVedtaksbrevTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 31L,
    beskrivelse = "Journalfører vedtaksbrev",
)
class JournalførVedtaksbrevTask(
    private val taskService: TaskService,
    private val behandlingService: BehandlingService,
    private val brevService: BrevService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val journalpostService: JournalpostService,
    private val brevmottakerVedtaksbrevRepository: BrevmottakerVedtaksbrevRepository,
    private val stegService: StegService,
    private val transactionHandler: TransactionHandler,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        val vedtaksbrev = brevService.hentBesluttetBrev(saksbehandling.id)

        val ikkeJournalførteBrevmottakere =
            brevmottakerVedtaksbrevRepository.findByBehandlingId(behandlingId).filter { it.journalpostId == null }
        ikkeJournalførteBrevmottakere.forEach { brevmottaker ->
            transactionHandler.runInNewTransaction {
                journalførVedtaksbrevForEnMottaker(vedtaksbrev, saksbehandling, brevmottaker)
            }
        }
        stegService.håndterSteg(behandlingId, StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV)
    }

    private fun journalførVedtaksbrevForEnMottaker(
        vedtaksbrev: Vedtaksbrev,
        saksbehandling: Saksbehandling,
        brevmottaker: BrevmottakerVedtaksbrev,
    ) {
        val dokument =
            Dokument(
                dokument = vedtaksbrev.beslutterPdf?.bytes ?: error("Mangler beslutterpdf"),
                filtype = Filtype.PDFA,
                dokumenttype = saksbehandling.stønadstype.dokumenttyper.vedtaksbrev,
                tittel = utledBrevtittel(saksbehandling),
            )

        val eksternReferanseId = "${saksbehandling.eksternId}-vedtaksbrev-${brevmottaker.id}"

        val arkviverDokumentRequest =
            ArkiverDokumentRequest(
                fnr = saksbehandling.ident,
                forsøkFerdigstill = true,
                hoveddokumentvarianter = listOf(dokument),
                fagsakId = saksbehandling.eksternFagsakId.toString(),
                journalførendeEnhet =
                    arbeidsfordelingService.hentNavEnhet(saksbehandling.ident, saksbehandling.stønadstype)?.enhetNr
                        ?: error("Fant ikke arbeidsfordelingsenhet"),
                eksternReferanseId = eksternReferanseId,
                avsenderMottaker = brevmottaker.mottaker.tilAvsenderMottaker(),
            )

        val arkiverDokumentResponse = opprettJournalpost(arkviverDokumentRequest, saksbehandling, eksternReferanseId)
        brevmottakerVedtaksbrevRepository.update(brevmottaker.copy(journalpostId = arkiverDokumentResponse.journalpostId))
    }

    private fun opprettJournalpost(
        arkviverDokumentRequest: ArkiverDokumentRequest,
        saksbehandling: Saksbehandling,
        eksternReferanseId: String,
    ): ArkiverDokumentResponse {
        val response =
            try {
                journalpostService.opprettJournalpost(arkviverDokumentRequest)
            } catch (e: ArkiverDokumentConflictException) {
                logger.warn(
                    "Konflikt ved arkivering av dokument. Vedtaksbrevet har sannsynligvis allerede blitt arkivert" +
                        " for behandlingId=${saksbehandling.id} med eksternReferanseId=$eksternReferanseId",
                )
                e.response
            }
        feilHvisIkke(response.ferdigstilt) {
            "Journalposten ble ikke ferdigstilt og kan derfor ikke distribueres"
        }
        return response
    }

    private fun utledBrevtittel(saksbehandling: Saksbehandling) = "Vedtak om ${saksbehandling.stønadstype.visningsnavn}"

    override fun onCompletion(task: Task) {
        taskService.save(DistribuerVedtaksbrevTask.opprettTask(BehandlingId.fromString(task.payload)))
    }

    companion object {
        fun opprettTask(behandlingId: BehandlingId): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties =
                    Properties().apply {
                        setProperty("behandlingId", behandlingId.toString())
                    },
            )

        const val TYPE = "journalførVedtaksbrev"
    }
}
