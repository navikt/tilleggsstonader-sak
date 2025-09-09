package no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveRepository
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.Oppgavestatus
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.util.oppgave
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GjenopprettOppgavePåBehandlingTaskTest {
    private val behandlingService: BehandlingService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val oppgaveRepository: OppgaveRepository = mockk()
    private lateinit var taskStep: GjenopprettOppgavePåBehandlingTask

    @BeforeEach
    fun setUp() {
        taskStep =
            GjenopprettOppgavePåBehandlingTask(
                behandligService = behandlingService,
                oppgaveService = oppgaveService,
                oppgaveRepository = oppgaveRepository,
            )
    }

    @Test
    fun `skal ignorere tidligere oppgave og opprette ny når siste er feilregistrert`() {
        val behandlingId = BehandlingId.random()
        val behandling = mockk<Behandling>(relaxed = true)
        every { behandling.id } returns behandlingId
        every { behandling.status } returns BehandlingStatus.FATTER_VEDTAK

        val sisteOppgave =
            oppgave(
                behandling = behandling,
                status = Oppgavestatus.FEILREGISTRERT,
            )

        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { oppgaveService.finnSisteOppgaveForBehandling(behandlingId) } returns sisteOppgave

        val opprettOppgaveSlot = slot<OpprettOppgave>()
        every { oppgaveService.opprettOppgave(behandlingId, capture(opprettOppgaveSlot)) } returns 1L

        val task: Task = GjenopprettOppgavePåBehandlingTask.opprettTask(behandlingId)

        taskStep.doTask(task)

        verify(exactly = 0) { oppgaveRepository.update(any()) }
        verify(exactly = 1) { oppgaveService.opprettOppgave(behandlingId, any()) }

        val ny = opprettOppgaveSlot.captured
        assertThat(ny.beskrivelse).contains("feilregistrert")
        assertThat(ny.oppgavetype).isEqualTo(Oppgavetype.GodkjenneVedtak)
    }

    @Test
    fun `skal ikke ignorere tidligere oppgave men opprette ny når siste er aapen`() {
        val behandlingId = BehandlingId.random()
        val behandling = mockk<Behandling>(relaxed = true)
        every { behandling.id } returns behandlingId
        every { behandling.status } returns BehandlingStatus.UTREDES

        val sisteOppgave =
            oppgave(
                behandling = behandling,
                status = Oppgavestatus.ÅPEN,
            )

        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { oppgaveService.finnSisteOppgaveForBehandling(behandlingId) } returns sisteOppgave

        val opprettOppgaveSlot = slot<OpprettOppgave>()
        every { oppgaveService.opprettOppgave(behandlingId, capture(opprettOppgaveSlot)) } returns 1L
        every { oppgaveRepository.update(any()) } returns sisteOppgave.copy(status = Oppgavestatus.IGNORERT)

        val task: Task = GjenopprettOppgavePåBehandlingTask.opprettTask(behandlingId)

        taskStep.doTask(task)

        verify(exactly = 1) {
            oppgaveRepository.update(
                withArg {
                    assertThat(it.status).isEqualTo(Oppgavestatus.IGNORERT)
                    assertThat(it.id).isEqualTo(sisteOppgave.id)
                },
            )
        }
        verify(exactly = 1) { oppgaveService.opprettOppgave(behandlingId, any()) }

        val ny = opprettOppgaveSlot.captured
        assertThat(ny.beskrivelse).contains("flyttet eller endret")
        assertThat(ny.oppgavetype).isEqualTo(Oppgavetype.BehandleSak)
    }
}
