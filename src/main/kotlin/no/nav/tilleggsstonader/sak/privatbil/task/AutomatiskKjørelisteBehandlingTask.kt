package no.nav.tilleggsstonader.sak.privatbil.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.kjørelistebrev.GenererKjørelistebrevDto
import no.nav.tilleggsstonader.sak.brev.kjørelistebrev.KjørelisteBehandlingBrevService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.privatbil.FullførKjørelistebehandlingSteg
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
    private val fullførKjørelistebehandlingSteg: FullførKjørelistebehandlingSteg,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)
        faktaGrunnlagService.opprettGrunnlagHvisDetIkkeEksisterer(behandlingId)
        stegService.håndterSteg(behandlingId, StegType.KJØRELISTE)
        stegService.håndterSteg(behandlingId, StegType.BEREGNING)
        stegService.håndterSteg(behandlingId, StegType.SIMULERING)
        kjørelisteBehandlingBrevService.genererOgLagreBrev(behandlingId, GenererKjørelistebrevDto(begrunnelse = null))
        stegService.håndterSteg(behandlingId, fullførKjørelistebehandlingSteg)
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
