package no.nav.tilleggsstonader.sak.privatbil.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.brev.kjørelistebrev.KjørelisteBehandlingBrevService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = AutomatiskKjørelisteBehandlingTask.TYPE,
    beskrivelse = "Automatisk behandling av kjøreliste uten avvik",
    maxAntallFeil = 3,
)
class AutomatiskKjørelisteBehandlingTask(
    private val stegService: StegService,
    private val taskService: TaskService,
    private val kjørelisteBehandlingBrevService: KjørelisteBehandlingBrevService,
    private val faktaGrunnlagService: FaktaGrunnlagService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)
        try {
            faktaGrunnlagService.opprettGrunnlagHvisDetIkkeEksisterer(behandlingId)
            stegService.håndterSteg(behandlingId, StegType.KJØRELISTE)
            stegService.håndterSteg(behandlingId, StegType.BEREGNING)
            stegService.håndterSteg(behandlingId, StegType.SIMULERING)
            kjørelisteBehandlingBrevService.genererOgLagreBrev(behandlingId)
            stegService.håndterSteg(behandlingId, StegType.FULLFØR_KJØRELISTE)
        } catch (e: Exception) {
            logger.warn(
                "Automatisk kjørelistebehandling feilet for behandling=$behandlingId. Oppretter oppgave for manuell behandling.",
                e,
            )
            taskService.save(
                OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                    OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                        behandlingId = behandlingId,
                        beskrivelse = "Skal behandles i TS-Sak",
                        prioritet = OppgavePrioritet.NORM,
                    ),
                ),
            )
        }
    }

    companion object {
        const val TYPE = "automatiskKjørelisteBehandling"

        fun opprettTask(behandlingId: BehandlingId): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties =
                    Properties().apply {
                        setProperty("behandlingId", behandlingId.toString())
                    },
            )
    }
}
