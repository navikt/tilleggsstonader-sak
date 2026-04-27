package no.nav.tilleggsstonader.sak.brev.kjørelistebrev

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
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakereService
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerUtil.tilAvsenderMottaker
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.brev.vedtaksbrev.DistribuerVedtaksbrevTask
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
    taskStepType = JournalførKjørelisteBehandlingBrevTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 31L,
    beskrivelse = "Journalfører vedtaksbrev for kjørelistebehandling",
)
class JournalførKjørelisteBehandlingBrevTask(
    private val taskService: TaskService,
    private val behandlingService: BehandlingService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val journalpostService: JournalpostService,
    private val brevmottakerVedtaksbrevRepository: BrevmottakerVedtaksbrevRepository,
    private val transactionHandler: TransactionHandler,
    private val kjørelisteBehandlingBrevService: KjørelisteBehandlingBrevService,
    private val brevmottakereService: BrevmottakereService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        feilHvisIkke(saksbehandling.type == BehandlingType.KJØRELISTE) {
            "Forventer at behandlingstype skal være ${BehandlingType.KJØRELISTE}"
        }

        val kjørelisteBrev = kjørelisteBehandlingBrevService.hentBrev(behandlingId)

        val ikkeJournalførteBrevmottakere =
            brevmottakereService.hentEllerOpprettBrevmottakere(behandlingId).filter { it.journalpostId == null }
        ikkeJournalførteBrevmottakere.forEach { brevmottaker ->
            transactionHandler.runInNewTransaction {
                journalførVedtaksbrevForEnMottaker(kjørelisteBrev, saksbehandling, brevmottaker)
            }
        }
    }

    private fun journalførVedtaksbrevForEnMottaker(
        kjørelisteBehandlingBrev: KjørelisteBehandlingBrev,
        saksbehandling: Saksbehandling,
        brevmottaker: BrevmottakerVedtaksbrev,
    ) {
        val dokument =
            Dokument(
                dokument = kjørelisteBehandlingBrev.pdf.bytes,
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
                    "Konflikt ved arkivering av dokument. Kjøreliste-brev har sannsynligvis allerede blitt arkivert" +
                        " for behandlingId=${saksbehandling.id} med eksternReferanseId=$eksternReferanseId",
                )
                e.response
            }
        feilHvisIkke(response.ferdigstilt) {
            "Journalposten ble ikke ferdigstilt og kan derfor ikke distribueres"
        }
        return response
    }

    // TODO - hva skal tittel være? Bør være forskjellig
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

        const val TYPE = "journalførKjørelisteBehandlingBrev"
    }
}
