package no.nav.tilleggsstonader.sak.utbetaling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.brev.JournalførVedtaksbrevTask
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Properties
import java.util.UUID

class VentePåStatusFraUtbetalingStegTest {

    val taskService = mockk<TaskService>()
    val steg = VentePåStatusFraUtbetalingSteg(taskService)

    val fagsak = fagsak()
    val behandling = behandling(fagsak)
    val saksbehandling = saksbehandling(fagsak, behandling)

    @Test
    internal fun `skal opprette journalførVedtaksbrevTask når den er ferdig`() {
        val taskSlot = slot<Task>()

        every { taskService.save(capture(taskSlot)) } returns mockk()

        steg.utførSteg(saksbehandling, null)

        Assertions.assertThat(taskSlot.captured.payload).isEqualTo(behandling.id.toString())
        Assertions.assertThat(taskSlot.captured.type).isEqualTo(JournalførVedtaksbrevTask.TYPE)
    }
}