package no.nav.tilleggsstonader.sak.behandlingsflyt.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class OpprettOppgaveForOpprettetBehandlingTaskTest {

    val behandlingService = mockk<BehandlingService>()
    val oppgaveService = mockk<OppgaveService>()
    val taskService = mockk<TaskService>()
    val opprettOppgaveForOpprettetBehandlingTask =
        OpprettOppgaveForOpprettetBehandlingTask(
            behandlingService = behandlingService,
            oppgaveService = oppgaveService,
            taskService = taskService
        )

    val oppgaveId = 1L

    val opprettTaskSlot = slot<Task>()

    @BeforeEach
    internal fun setUp() {
        every { oppgaveService.opprettOppgave(any(), any()) } returns oppgaveId
        every { taskService.save(capture(opprettTaskSlot)) } answers { firstArg() }
    }

    @EnumSource(
        value = BehandlingStatus::class,
        names = ["OPPRETTET", "UTREDES"],
        mode = EnumSource.Mode.INCLUDE,
    )
    @ParameterizedTest
    internal fun `Skal opprette oppgave hvis behandlingen har status opprettet eller utredes`(behandlingStatus: BehandlingStatus) {
        val behandling = mockBehandling(behandlingStatus)

        opprettOppgaveForOpprettetBehandlingTask.doTask(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandling.id,
                    "",
                ),
            ),
        )

        verify(exactly = 1) { oppgaveService.opprettOppgave(any(), any()) }
    }

    @EnumSource(
        value = BehandlingStatus::class,
        names = ["OPPRETTET", "UTREDES"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    @ParameterizedTest
    internal fun `Skal ikke opprette oppgave hvis behandlingen ikke har status opprettet eller utredes`(behandlingStatus: BehandlingStatus) {
        val behandling = mockBehandling(behandlingStatus)

        opprettOppgaveForOpprettetBehandlingTask.doTask(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandling.id,
                    "",
                ),
            ),
        )

        verify(exactly = 0) { oppgaveService.opprettOppgave(any(), any()) }
    }

    private fun mockBehandling(status: BehandlingStatus): Saksbehandling {
        val behandling = saksbehandling(status = status)
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        return behandling
    }
}
