package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class IverksettBehandlingMånedTaskTest {

    private val behandlingService = mockk<BehandlingService>()
    private val iverksettService = mockk<IverksettService>(relaxed = true)
    private val taskStep = IverksettBehandlingMånedTask(behandlingService, iverksettService)

    val fagsak = fagsak()
    val behandling = behandling(fagsak, vedtakstidspunkt = osloNow().minusDays(1))
    val behandling2 = behandling(fagsak, forrigeBehandlingId = behandling.id, vedtakstidspunkt = osloNow())
    val måned = YearMonth.now()

    @BeforeEach
    fun setUp() {
        mockHentBehandling(behandling)
    }

    @Test
    fun `skal kalle på iverksetting`() {
        mockFinnSisteIverksatteBehandling(behandling)
        val task = IverksettBehandlingMånedTask.opprettTask(behandling.id, måned)
        val iverksettingId = UUID.fromString(task.metadata.getProperty("iverksettingId"))

        taskStep.doTask(task)

        verify(exactly = 1) { iverksettService.iverksett(behandling.id, iverksettingId, måned) }
    }

    @Test
    fun `skal feile hvis det finnes en behandling som er iverksatt etter behandlingen for tasken`() {
        mockFinnSisteIverksatteBehandling(behandling2)

        val task = IverksettBehandlingMånedTask.opprettTask(behandlingId = behandling.id, måned)
        assertThatThrownBy {
            taskStep.doTask(task)
        }.hasMessageContaining("En revurdering har erstattet denne behandlingen.")
    }

    private fun mockHentBehandling(behandling: Behandling) {
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
    }

    private fun mockFinnSisteIverksatteBehandling(behandling: Behandling) {
        every { behandlingService.finnSisteIverksatteBehandling(behandling.fagsakId) } returns behandling
    }
}
