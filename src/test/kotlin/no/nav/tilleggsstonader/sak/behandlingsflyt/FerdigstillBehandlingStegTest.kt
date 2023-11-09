package no.nav.tilleggsstonader.sak.behandlingsflyt

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FerdigstillBehandlingStegTest {

    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val taskService = mockk<TaskService>()

    private val task = FerdigstillBehandlingSteg(behandlingService)

    private val fagsak = fagsak()
    private val taskSlot = mutableListOf<Task>()

    @BeforeEach
    internal fun setUp() {
        taskSlot.clear()
        every { taskService.save(capture(taskSlot)) } answers { firstArg() }
    }

    @Test
    internal fun `skal opprette publiseringstask og behandlingsstatistikkTask hvis behandlingen er førstegagsbehandling`() {
        task.utførSteg(saksbehandling(fagsak, behandling(fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING)), null)
        verify(exactly = 2) { taskService.save(any()) }
    }

    @Test
    internal fun `skal kaste feil hvis behandlingen er av andre typer`() {
        Assertions.assertThat(
            Assertions.catchThrowable {
                task.utførSteg(
                    saksbehandling(fagsak, behandling(fagsak, type = BehandlingType.REVURDERING)),
                    null,
                )
            },
        )
    }
}
