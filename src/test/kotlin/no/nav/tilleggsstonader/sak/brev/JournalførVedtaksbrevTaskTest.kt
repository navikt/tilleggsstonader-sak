package no.nav.tilleggsstonader.sak.brev

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.util.Properties
import java.util.UUID

class JournalførVedtaksbrevTaskTest {
    val stegService = mockk<StegService>()
    val journalførVedtaksbrevSteg = mockk<JournalførVedtaksbrevSteg>()
    val taskService = mockk<TaskService>()

    private val journalførVedtaksbrevTask = JournalførVedtaksbrevTask(stegService, journalførVedtaksbrevSteg, taskService)

    @Test
    internal fun `skal opprette distribuerVedtaksbrevTask når den er ferdig`() {
        val behandlingId = UUID.randomUUID()
        val task = Task(JournalførVedtaksbrevTask.TYPE, behandlingId.toString(), Properties())
        val taskSlot = slot<Task>()

        every { taskService.save(capture(taskSlot)) } returns mockk()

        journalførVedtaksbrevTask.onCompletion(task)

        assertThat(taskSlot.captured.payload).isEqualTo(behandlingId.toString())
        assertThat(taskSlot.captured.type).isEqualTo(DistribuerVedtaksbrevTask.TYPE)
    }
}
