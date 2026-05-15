package no.nav.tilleggsstonader.sak.behandlingsflyt

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.interntVedtak.InterntVedtakTask
import no.nav.tilleggsstonader.sak.privatbil.varsel.KjørelistevarselService
import no.nav.tilleggsstonader.sak.privatbil.varsel.SendKjorelistevarselTilBrukerTask
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

class FerdigstillBehandlingStegTest {
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val taskService = mockk<TaskService>()
    private val kjørelistevarselService = mockk<KjørelistevarselService>()

    private val steg = FerdigstillBehandlingSteg(behandlingService, taskService, kjørelistevarselService)

    private val taskSlot = mutableListOf<Task>()
    private val fagsak = fagsak()
    private val behandling = saksbehandling(fagsak, behandling(fagsak, type = BehandlingType.REVURDERING))

    @BeforeEach
    internal fun setUp() {
        taskSlot.clear()
        every { taskService.save(capture(taskSlot)) } answers { firstArg() }
        every { kjørelistevarselService.skalSendeKjørelistevarselVedFerdigstillingAvBehandling(any()) } returns false
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

    @Test
    fun `skal ikke opprette task for internt vedtak for kjørelistebehandling`() {
        val kjørelistebehandling = saksbehandling(fagsak, behandling(fagsak, type = BehandlingType.KJØRELISTE))

        steg.utførSteg(kjørelistebehandling, null)

        val tasks = taskSlot.filter { it.type == InterntVedtakTask.TYPE }
        assertThat(tasks).isEmpty()
    }

    @Test
    fun `skal opprette task for å lage varsel til mitt nav når det finnes tilgjengelige kjørelister`() {
        every { kjørelistevarselService.skalSendeKjørelistevarselVedFerdigstillingAvBehandling(any()) } returns true
        steg.utførSteg(behandling, null)

        val tasks = taskSlot.filter { it.type == SendKjorelistevarselTilBrukerTask.TYPE }
        assertThat(tasks).hasSize(1)

        val sendKjørelistevarselTaskdata =
            jsonMapper.readValue<SendKjorelistevarselTilBrukerTask.SendKjørelistevarselTilBrukerTaskData>(
                tasks.single().payload,
            )
        assertThat(sendKjørelistevarselTaskdata.fagsakPersonId)
            .isEqualTo(fagsak.fagsakPersonId)
    }
}
