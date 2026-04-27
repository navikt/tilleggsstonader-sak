package no.nav.tilleggsstonader.sak.brev.vedtaksbrev

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.JournalførVedtaksbrevService
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
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
    private val brevmottakerVedtaksbrevRepository: BrevmottakerVedtaksbrevRepository,
    private val stegService: StegService,
    private val journalførVedtaksbrevService: JournalførVedtaksbrevService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val vedtaksbrev = brevService.hentBesluttetBrev(saksbehandling.id)

        journalførVedtaksbrevService.journalførForAlleMottakere(
            pdfBytes = vedtaksbrev.beslutterPdf?.bytes ?: error("Mangler beslutterpdf"),
            saksbehandling = saksbehandling,
            brevmottakere = brevmottakerVedtaksbrevRepository.findByBehandlingId(behandlingId),
            brevtittel = "Vedtak om ${saksbehandling.stønadstype.visningsnavn}",
        )
        stegService.håndterSteg(behandlingId, StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV)
    }

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
