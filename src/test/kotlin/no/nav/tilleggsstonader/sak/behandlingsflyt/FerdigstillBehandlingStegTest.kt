package no.nav.tilleggsstonader.sak.behandlingsflyt

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.interntVedtak.InterntVedtakTask
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FerdigstillBehandlingStegTest {

    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val taskService = mockk<TaskService>()

    private val steg = FerdigstillBehandlingSteg(behandlingService, taskService)

    private val taskSlot = mutableListOf<Task>()
    private val fagsak = fagsak()
    private val behandling = saksbehandling(fagsak, behandling(fagsak, type = BehandlingType.REVURDERING))

    @BeforeEach
    internal fun setUp() {
        taskSlot.clear()
        every { taskService.save(capture(taskSlot)) } answers { firstArg() }
    }

    @Test
    internal fun `skal oppdatere status på behandlingen`() {
        steg.utførSteg(behandling, null)
        verify { behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.FERDIGSTILT) }
    }

    @Test
    fun `skal opprette task for å lage internt vedtak`() {
        steg.utførSteg(behandling, null)
        val tasks = taskSlot.filter { it.type == InterntVedtakTask.TYPE }
        assertThat(tasks).hasSize(1)
        assertThat(tasks[0].payload).isEqualTo(behandling.id.toString())
    }
}
