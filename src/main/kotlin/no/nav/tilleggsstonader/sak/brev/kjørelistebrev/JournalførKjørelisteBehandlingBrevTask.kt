package no.nav.tilleggsstonader.sak.brev.kjørelistebrev

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.brev.JournalførVedtaksbrevService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakereService
import no.nav.tilleggsstonader.sak.brev.vedtaksbrev.DistribuerVedtaksbrevTask
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
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
    private val kjørelisteBehandlingBrevService: KjørelisteBehandlingBrevService,
    private val brevmottakereService: BrevmottakereService,
    private val journalførVedtaksbrevService: JournalførVedtaksbrevService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        feilHvisIkke(saksbehandling.type == BehandlingType.KJØRELISTE) {
            "Forventer at behandlingstype skal være ${BehandlingType.KJØRELISTE}"
        }

        val brev = kjørelisteBehandlingBrevService.hentBrev(behandlingId)

        journalførVedtaksbrevService.journalførForAlleMottakere(
            pdfBytes = brev.pdf.bytes,
            saksbehandling = saksbehandling,
            brevmottakere = brevmottakereService.hentEllerOpprettBrevmottakere(behandlingId),
            // TODO - hva skal tittel være? Bør være forskjellig fra rammevedtak-brev
            brevtittel = "Vedtak om ${saksbehandling.stønadstype.visningsnavn}",
        )
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

        const val TYPE = "journalførKjørelisteBehandlingBrev"
    }
}
