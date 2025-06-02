package no.nav.tilleggsstonader.sak.brev.vedtaksbrev

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.prosessering.domene.Loggtype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskLogg
import no.nav.familie.prosessering.error.RekjørSenereException
import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.libs.http.client.ProblemDetailException
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerTestUtil.mottakerPerson
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.MottakerRolle
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.client.HttpClientErrorException

class DistribuerVedtaksbrevTaskTest {
    val brevmottakerVedtaksbrevRepository = mockk<BrevmottakerVedtaksbrevRepository>()
    val journalpostClient = mockk<JournalpostClient>()
    val stegService = mockk<StegService>()
    val taskService = mockk<TaskService>(relaxed = true)
    val brevSteg = BrevSteg(taskService)
    val behandlingSerice = mockk<BehandlingService>()

    val saksbehandling = saksbehandling(steg = StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV)
    val distribuerVedtaksbrevTask =
        DistribuerVedtaksbrevTask(
            brevmottakerVedtaksbrevRepository,
            journalpostClient,
            TransactionHandler(),
            taskService,
        )
    val task = Task(type = DistribuerVedtaksbrevTask.TYPE, payload = saksbehandling.id.toString())

    val journalpostIdA = "journalpostIdA"
    val journalpostIdB = "journalpostIdB"

    @BeforeEach
    fun setUp() {
        every { behandlingSerice.hentSaksbehandling(saksbehandling.id) } returns saksbehandling
        every { stegService.håndterSteg(any<BehandlingId>(), brevSteg) } returns mockk()
    }

    @Test
    fun `skal distribuere brev til alle brevmottakere`() {
        val distribuerrequestSlots = mutableListOf<DistribuerJournalpostRequest>()
        val brevmottakereSlots = mutableListOf<BrevmottakerVedtaksbrev>()

        every { brevmottakerVedtaksbrevRepository.findByBehandlingId(saksbehandling.id) } returns
            listOf(
                BrevmottakerVedtaksbrev(
                    behandlingId = saksbehandling.id,
                    mottaker = mottakerPerson(ident = saksbehandling.ident),
                    journalpostId = journalpostIdA,
                    bestillingId = null,
                ),
                BrevmottakerVedtaksbrev(
                    behandlingId = saksbehandling.id,
                    mottaker =
                        mottakerPerson(
                            mottakerRolle = MottakerRolle.VERGE,
                            ident = "identAnnenMottaker",
                        ),
                    journalpostId = journalpostIdB,
                    bestillingId = null,
                ),
            )

        every { brevmottakerVedtaksbrevRepository.update(capture(brevmottakereSlots)) } returns mockk()

        val bestillingIdA = "bestillingId1"
        val bestillingIdB = "bestillingId2"
        every {
            journalpostClient.distribuerJournalpost(
                capture(distribuerrequestSlots),
                null,
            )
        } returns bestillingIdA andThen bestillingIdB

        distribuerVedtaksbrevTask.doTask(task)

        assertThat(distribuerrequestSlots.size).isEqualTo(2)
        assertThat(brevmottakereSlots.size).isEqualTo(2)
        assertThat(distribuerrequestSlots[0].journalpostId).isEqualTo(journalpostIdA)
        assertThat(distribuerrequestSlots[1].journalpostId).isEqualTo(journalpostIdB)
        assertThat(brevmottakereSlots[0].bestillingId).isEqualTo(bestillingIdA)
        assertThat(brevmottakereSlots[1].bestillingId).isEqualTo(bestillingIdB)
    }

    @Test
    fun `skal ikke distribuere brev som allerede er distribuert`() {
        val distribuerrequestSlots = mutableListOf<DistribuerJournalpostRequest>()
        val brevmottakereSlots = mutableListOf<BrevmottakerVedtaksbrev>()

        every { brevmottakerVedtaksbrevRepository.findByBehandlingId(saksbehandling.id) } returns
            listOf(
                BrevmottakerVedtaksbrev(
                    behandlingId = saksbehandling.id,
                    mottaker = mottakerPerson(ident = saksbehandling.ident),
                    journalpostId = journalpostIdA,
                    bestillingId = "alleredeDistribuertId",
                ),
                BrevmottakerVedtaksbrev(
                    behandlingId = saksbehandling.id,
                    mottaker =
                        mottakerPerson(
                            mottakerRolle = MottakerRolle.VERGE,
                            ident = "identAnnenMottaker",
                        ),
                    journalpostId = journalpostIdB,
                    bestillingId = null,
                ),
            )

        every { brevmottakerVedtaksbrevRepository.update(capture(brevmottakereSlots)) } returns mockk()

        val bestillingIdA = "bestillingId1"
        every { journalpostClient.distribuerJournalpost(capture(distribuerrequestSlots), null) } returns bestillingIdA

        distribuerVedtaksbrevTask.doTask(task)

        verify(exactly = 1) { journalpostClient.distribuerJournalpost(any(), any()) }

        assertThat(distribuerrequestSlots.size).isEqualTo(1)
        assertThat(brevmottakereSlots.size).isEqualTo(1)
        assertThat(brevmottakereSlots[0].bestillingId).isEqualTo(bestillingIdA)
    }

    @Nested
    inner class `Mottaker er død og mangler adresse` {
        @Test
        internal fun `skal rekjøre senere hvis man får GONE fra dokdist`() {
            val distribuerrequestSlots = mutableListOf<DistribuerJournalpostRequest>()

            every { brevmottakerVedtaksbrevRepository.findByBehandlingId(saksbehandling.id) } returns
                listOf(
                    BrevmottakerVedtaksbrev(
                        behandlingId = saksbehandling.id,
                        mottaker = mottakerPerson(ident = saksbehandling.ident),
                        journalpostId = journalpostIdA,
                        bestillingId = null,
                    ),
                )

            every {
                journalpostClient.distribuerJournalpost(
                    capture(distribuerrequestSlots),
                    null,
                )
            } throws HttpStatus.GONE.problemDetailException()

            val rekjørSenereException = catchThrowableOfType<RekjørSenereException> { distribuerVedtaksbrevTask.doTask(task) }

            assertThat(rekjørSenereException.triggerTid)
                .isBetween(osloNow().plusDays(6), osloNow().plusDays(8))
            assertThat(rekjørSenereException.årsak).startsWith("Mottaker er død")

            verify(exactly = 1) { journalpostClient.distribuerJournalpost(any(), any()) }
            verify(exactly = 0) { brevmottakerVedtaksbrevRepository.update(any()) }
        }

        @Test
        internal fun `skal feile hvis tasken har kjørt over 26 ganger`() {
            val distribuerrequestSlots = mutableListOf<DistribuerJournalpostRequest>()

            every { brevmottakerVedtaksbrevRepository.findByBehandlingId(saksbehandling.id) } returns
                listOf(
                    BrevmottakerVedtaksbrev(
                        behandlingId = saksbehandling.id,
                        mottaker = mottakerPerson(ident = saksbehandling.ident),
                        journalpostId = journalpostIdA,
                        bestillingId = null,
                    ),
                )

            every {
                journalpostClient.distribuerJournalpost(
                    capture(distribuerrequestSlots),
                    null,
                )
            } throws HttpStatus.GONE.problemDetailException()

            val taskLogg = (1..27).map { TaskLogg(taskId = task.id, type = Loggtype.KLAR_TIL_PLUKK, melding = "Mottaker er død") }
            every { taskService.findTaskLoggByTaskId(any()) } returns taskLogg

            val taskExceptionUtenStackTrace = catchThrowableOfType<TaskExceptionUtenStackTrace> { distribuerVedtaksbrevTask.doTask(task) }

            assertThat(taskExceptionUtenStackTrace).hasMessageStartingWith("Nådd max antall rekjøringer - 26")

            verify(exactly = 1) { journalpostClient.distribuerJournalpost(any(), any()) }
            verify(exactly = 0) { brevmottakerVedtaksbrevRepository.update(any()) }
        }
    }

    private fun HttpStatus.problemDetailException() =
        ProblemDetailException(
            detail = ProblemDetail.forStatus(this),
            responseException = HttpClientErrorException.create(this, "", HttpHeaders(), byteArrayOf(), null),
            httpStatus = this,
        )
}
