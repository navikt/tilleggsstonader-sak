package no.nav.tilleggsstonader.sak.hendelser.journalføring

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.prosessering.internal.TaskService
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.sak.hendelser.ConsumerRecordUtil.lagConsumerRecord
import no.nav.tilleggsstonader.sak.hendelser.Hendelse
import no.nav.tilleggsstonader.sak.hendelser.TypeHendelse
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.HendelseRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment

class JournalhendelseKafkaListenerTest {
    val hendelseRepository = spyk(HendelseRepositoryFake())
    val taskService = mockk<TaskService>(relaxed = true)

    val listener =
        JournalhendelseKafkaListener(
            hendelseRepository = hendelseRepository,
            transactionHandler = TransactionHandler(),
            taskService = taskService,
        )

    val ack = mockk<Acknowledgment>(relaxed = true)

    @BeforeEach
    fun setUp() {
        hendelseRepository.deleteAll()
    }

    @Test
    fun `skal behandle innkommende journalposter`() {
        listener.listen(lagConsumerRecord("key", record()), ack)

        with(hendelseRepository.findAll().single()) {
            assertThat(id).isEqualTo("hendelseId")
            assertThat(type).isEqualTo(TypeHendelse.JOURNALPOST)
        }
        verify(exactly = 1) { hendelseRepository.existsByTypeAndId(TypeHendelse.JOURNALPOST, "hendelseId") }
        verify(exactly = 1) { taskService.save(any()) }
        verify(exactly = 1) { hendelseRepository.insert(any()) }
        verify(exactly = 1) { ack.acknowledge() }
    }

    @Test
    fun `skal ikke behandle hendelse hvis den allerede er behandlet`() {
        hendelseRepository.insert(Hendelse(TypeHendelse.JOURNALPOST, "hendelseId"))
        clearMocks(hendelseRepository, answers = false, recordedCalls = true) //

        listener.listen(lagConsumerRecord("key", record()), ack)

        verify(exactly = 1) { hendelseRepository.existsByTypeAndId(any(), any()) }
        verify(exactly = 0) { taskService.save(any()) }
        verify(exactly = 0) { hendelseRepository.insert(any()) }
        verify(exactly = 1) { ack.acknowledge() }
    }

    @Test
    fun `skal ikke behandle annet tema enn tilleggsstønader`() {
        listener.listen(lagConsumerRecord("key", record(tema = "ENF")), ack)

        verify(exactly = 0) { hendelseRepository.existsByTypeAndId(any(), any()) }
        verify(exactly = 0) { taskService.save(any()) }
        verify(exactly = 0) { hendelseRepository.insert(any()) }
        verify(exactly = 1) { ack.acknowledge() }
    }

    private fun record(
        tema: String? = Tema.TSO.name,
        hendelseId: String? = "hendelseId",
        hendelseType: String? = "JournalpostMottatt",
        journalpostId: Long = 1,
    ): JournalfoeringHendelseRecord =
        JournalfoeringHendelseRecord().apply {
            temaNytt = tema
            hendelsesId = hendelseId
            hendelsesType = hendelseType
            this.journalpostId = journalpostId
        }
}
