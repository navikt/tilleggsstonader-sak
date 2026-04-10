package no.nav.tilleggsstonader.sak.brev.frittstående

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.prosessering.domene.Loggtype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskLogg
import no.nav.familie.prosessering.error.RekjørSenereException
import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerFrittståendeBrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerTestUtil.mottakerPerson
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerFrittståendeBrev
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class DistribuerFrittståendeBrevTaskTest {
    private val brevmottakerFrittståendeBrevRepository = mockk<BrevmottakerFrittståendeBrevRepository>()
    private val distribuerFrittståendeBrevService = mockk<DistribuerFrittståendeBrevService>()
    private val taskService = mockk<TaskService>(relaxed = true)

    private val distribuerFrittståendeBrevTask =
        DistribuerFrittståendeBrevTask(
            brevmottakerFrittståendeBrevRepository = brevmottakerFrittståendeBrevRepository,
            distribuerFrittståendeBrevService = distribuerFrittståendeBrevService,
            taskService = taskService,
        )

    private val fagsakId = FagsakId.random()
    private val journalpostId = "journalpostId123"
    private val mottakerId = UUID.randomUUID()

    private val payload =
        DistribuerFrittståendeBrevPayload(
            fagsakId = fagsakId,
            journalpostId = journalpostId,
            mottakerId = mottakerId,
        )

    private val task =
        Task(
            type = DistribuerFrittståendeBrevTask.TYPE,
            payload = jsonMapper.writeValueAsString(payload),
        )

    private val brevmottaker =
        BrevmottakerFrittståendeBrev(
            id = mottakerId,
            fagsakId = fagsakId,
            mottaker = mottakerPerson(ident = "12345678901"),
            journalpostId = journalpostId,
            bestillingId = null,
        )

    @BeforeEach
    fun setUp() {
        every { brevmottakerFrittståendeBrevRepository.findById(mottakerId) } returns Optional.of(brevmottaker)
    }

    @Test
    fun `skal distribuere brev via service`() {
        every {
            distribuerFrittståendeBrevService.distribuerBrev(brevmottaker)
        } returns DistribuerFrittståendeBrevService.ResultatDistribusjon.BrevDistribuert

        distribuerFrittståendeBrevTask.doTask(task)
    }

    @Nested
    inner class `Rekjøring senere` {
        @Test
        fun `skal rekjøre senere hvis mottaker er død og mangler adresse`() {
            every {
                distribuerFrittståendeBrevService.distribuerBrev(brevmottaker)
            } returns DistribuerFrittståendeBrevService.ResultatDistribusjon.FeiletFordiMottakerErDødOgManglerAdresse("Gone")

            val rekjørSenereException =
                catchThrowableOfType<RekjørSenereException> { distribuerFrittståendeBrevTask.doTask(task) }

            assertThat(rekjørSenereException.triggerTid)
                .isBetween(LocalDateTime.now().plusDays(6), LocalDateTime.now().plusDays(8))
            assertThat(rekjørSenereException.årsak).startsWith("Mottaker er død")
        }

        @Test
        fun `skal feile hvis tasken har kjørt over 26 ganger`() {
            every {
                distribuerFrittståendeBrevService.distribuerBrev(brevmottaker)
            } returns DistribuerFrittståendeBrevService.ResultatDistribusjon.FeiletFordiMottakerErDødOgManglerAdresse("Gone")

            val taskLogg =
                (1..27).map { TaskLogg(taskId = task.id, type = Loggtype.KLAR_TIL_PLUKK, melding = "Mottaker er død") }
            every { taskService.findTaskLoggByTaskId(any()) } returns taskLogg

            val taskException =
                catchThrowableOfType<TaskExceptionUtenStackTrace> { distribuerFrittståendeBrevTask.doTask(task) }

            assertThat(taskException).hasMessageStartingWith("Nådd max antall rekjøringer - 26")
        }
    }
}
