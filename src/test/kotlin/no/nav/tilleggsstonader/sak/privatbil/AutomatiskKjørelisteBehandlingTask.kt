package no.nav.tilleggsstonader.sak.privatbil

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.brev.kjørelistebrev.KjørelisteBehandlingBrevService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.privatbil.task.AutomatiskKjørelisteBehandlingTask
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

class AutomatiskKjørelisteBehandlingTask {
    private val stegService = mockk<StegService>(relaxed = true)
    private val taskService = mockk<TaskService>(relaxed = true)
    private val kjørelisteBehandlingBrevService = mockk<KjørelisteBehandlingBrevService>()
    private val faktaGrunnlagService = mockk<FaktaGrunnlagService>(relaxed = true)

    private val taskStep =
        AutomatiskKjørelisteBehandlingTask(
            stegService,
            taskService,
            kjørelisteBehandlingBrevService,
            faktaGrunnlagService,
        )

    @Test
    fun `skal kjøre steg for automatisk kjørelistebehandling`() {
        val behandlingId = BehandlingId.random()
        every { kjørelisteBehandlingBrevService.genererOgLagreBrev(behandlingId) } returns mockk()

        taskStep.doTask(AutomatiskKjørelisteBehandlingTask.opprettTask(behandlingId))

        verifyOrder {
            faktaGrunnlagService.opprettGrunnlagHvisDetIkkeEksisterer(behandlingId)
            stegService.håndterSteg(behandlingId, StegType.KJØRELISTE)
            stegService.håndterSteg(behandlingId, StegType.BEREGNING)
            stegService.håndterSteg(behandlingId, StegType.SIMULERING)
            kjørelisteBehandlingBrevService.genererOgLagreBrev(behandlingId)
            stegService.håndterSteg(behandlingId, StegType.FULLFØR_KJØRELISTE)
        }
        verify(exactly = 0) { taskService.save(any()) }
    }

    @Test
    fun `skal opprette oppgave når automatisk behandling feiler`() {
        val behandlingId = BehandlingId.random()
        val taskSlot = slot<Task>()

        every {
            stegService.håndterSteg(
                behandlingId,
                StegType.SIMULERING,
            )
        } throws IllegalStateException("Simulering nede")
        every { taskService.save(capture(taskSlot)) } answers { firstArg() }

        taskStep.doTask(AutomatiskKjørelisteBehandlingTask.opprettTask(behandlingId))

        assertThat(taskSlot.captured.type).isEqualTo(OpprettOppgaveForOpprettetBehandlingTask.TYPE)

        val taskData =
            jsonMapper.readValue<OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData>(taskSlot.captured.payload)
        assertThat(taskData.behandlingId).isEqualTo(behandlingId)
        assertThat(taskData.prioritet).isEqualTo(OppgavePrioritet.NORM)
        assertThat(taskData.beskrivelse).isEqualTo("Skal behandles i TS-Sak")
    }
}
