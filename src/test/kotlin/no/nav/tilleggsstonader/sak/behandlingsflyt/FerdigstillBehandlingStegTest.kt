package no.nav.tilleggsstonader.sak.behandlingsflyt

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FerdigstillBehandlingStegTest {

    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val taskService = mockk<TaskService>()

    private val steg = FerdigstillBehandlingSteg(behandlingService)

    private val fagsak = fagsak()
    private val taskSlot = mutableListOf<Task>()

    @BeforeEach
    internal fun setUp() {
        taskSlot.clear()
        every { taskService.save(capture(taskSlot)) } answers { firstArg() }
    }

    @Test
    internal fun `skal oppdatere status på behandlingen`() {
        val behandling = saksbehandling(fagsak, behandling(fagsak, type = BehandlingType.REVURDERING))
        steg.utførSteg(behandling, null)
        verify { behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.FERDIGSTILT) }
    }
}
