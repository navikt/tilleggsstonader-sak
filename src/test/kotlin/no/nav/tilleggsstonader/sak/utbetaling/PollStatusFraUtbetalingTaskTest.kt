package no.nav.tilleggsstonader.sak.utbetaling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.brev.JournalførVedtaksbrevTask
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Properties
import java.util.UUID

class PollStatusFraUtbetalingTaskTest {

    val stegService = mockk<StegService>()
    val ventePåStatusFraUtbetalingSteg = mockk<VentePåStatusFraUtbetalingSteg>()
    val taskService = mockk<TaskService>()

    private val pollStatusFraUtbetalingTask =
        PollStatusFraUtbetalingTask(stegService, ventePåStatusFraUtbetalingSteg, taskService)

    @Test
    internal fun `skal opprette journalførVedtaksbrevTask når den er ferdig`() {
        val behandlingId = UUID.randomUUID()
        val task = Task(PollStatusFraUtbetalingTask.TYPE, behandlingId.toString(), Properties())
        val taskSlot = slot<Task>()

        every { taskService.save(capture(taskSlot)) } returns mockk()

        pollStatusFraUtbetalingTask.onCompletion(task)

        Assertions.assertThat(taskSlot.captured.payload).isEqualTo(behandlingId.toString())
        Assertions.assertThat(taskSlot.captured.type).isEqualTo(JournalførVedtaksbrevTask.TYPE)
    }
}
