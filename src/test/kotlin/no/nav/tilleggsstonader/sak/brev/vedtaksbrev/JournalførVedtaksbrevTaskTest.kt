package no.nav.tilleggsstonader.sak.brev.vedtaksbrev

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingTestUtil
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerUtil.mottakerPerson
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.Mottaker
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.MottakerRolle
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.MottakerType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.journalføring.ArkiverDokumentConflictException
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vedtaksbrev
import org.assertj.core.api.Assertions
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.http.HttpTimeoutException
import java.util.*

class JournalførVedtaksbrevTaskTest {

    val taskService = mockk<TaskService>()

    val behandlingService = mockk<BehandlingService>()
    val brevService = mockk<BrevService>()
    val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    val journalpostService = mockk<JournalpostService>()
    val brevmottakerVedtaksbrevRepository = mockk<BrevmottakerVedtaksbrevRepository>()

    private val journalførVedtaksbrevTask = JournalførVedtaksbrevTask(
        taskService,
        behandlingService,
        brevService,
        arbeidsfordelingService,
        journalpostService,
        brevmottakerVedtaksbrevRepository,
        TransactionHandler(),
    )

    val saksbehandling = saksbehandling()
    val task = Task(JournalførVedtaksbrevTask.TYPE, saksbehandling.id.toString(), Properties())

    @BeforeEach
    fun setUp() {
        every { brevService.hentBesluttetBrev(saksbehandling.id) } returns vedtaksbrev(behandlingId = saksbehandling.id)
        every { arbeidsfordelingService.hentNavEnhet(any()) } returns ArbeidsfordelingTestUtil.ENHET_NASJONAL_NAY
        every { brevmottakerVedtaksbrevRepository.insert(any()) } returns mockk()
        every { brevmottakerVedtaksbrevRepository.findByBehandlingId(saksbehandling.id) } returns listOf(
            BrevmottakerVedtaksbrev(
                behandlingId = saksbehandling.id,
                mottaker = mottakerPerson(ident = saksbehandling.ident),
            ),
        )
        every { behandlingService.hentSaksbehandling(saksbehandling.id) } returns saksbehandling
    }

    @Test
    internal fun `skal feile dersom kall mot dokarkiv feiler, og feilen ikke er 409 Conflict`() {
        val feil = HttpTimeoutException("")

        every { journalpostService.opprettJournalpost(any()) } throws feil

        Assertions.assertThatThrownBy {
            journalførVedtaksbrevTask.doTask(task)
        }.isInstanceOf(HttpTimeoutException::class.java)
    }

    @Test
    internal fun `skal ikke feile dersom kall mot dokarkiv feiler, og feilen er 409 Conflict`() {
        val exception = ArkiverDokumentConflictException(ArkiverDokumentResponse("journalpostID", true, null))
        val brevmottaker = BrevmottakerVedtaksbrev(
            behandlingId = saksbehandling.id,
            mottaker = mottakerPerson(ident = saksbehandling.ident),
        )
        every { brevmottakerVedtaksbrevRepository.update(any()) } returns brevmottaker
        every { journalpostService.opprettJournalpost(any()) } throws exception

        journalførVedtaksbrevTask.doTask(task)

        verify { brevmottakerVedtaksbrevRepository.update(any()) }
    }

    @Test
    internal fun `skal opprette distribuerVedtaksbrevTask når den er ferdig`() {
        val taskSlot = slot<Task>()

        every { taskService.save(capture(taskSlot)) } returns mockk()

        journalførVedtaksbrevTask.onCompletion(task)

        assertThat(taskSlot.captured.payload).isEqualTo(saksbehandling.id.toString())
        assertThat(taskSlot.captured.type).isEqualTo(DistribuerVedtaksbrevTask.TYPE)
    }

    @Test
    internal fun `dersom det finnes én brevmottaker skal opprettJournalpost og brevmottakerRepository-update kjøres én gang hver`() {
        val brevmottaker = mockk<BrevmottakerVedtaksbrev>()
        every { journalpostService.opprettJournalpost(any()) } returns ArkiverDokumentResponse(
            "journalpostID",
            true,
            null,
        )
        every { brevmottakerVedtaksbrevRepository.update(any()) } returns brevmottaker

        journalførVedtaksbrevTask.doTask(task)

        verify(exactly = 1) { journalpostService.opprettJournalpost(any()) }
        verify(exactly = 1) { brevmottakerVedtaksbrevRepository.update(any()) }
    }

    @Test
    internal fun `dersom det finnes to brevmottakere skal opprettJournalpost og brevmottakerRepository-update kjøres to ganger hver`() {
        val brevmottakerDuplikat = BrevmottakerVedtaksbrev(
            behandlingId = saksbehandling.id,
            mottaker = Mottaker(
                mottakerRolle = MottakerRolle.BRUKER,
                mottakerType = MottakerType.PERSON,
                ident = saksbehandling.ident,
            ),
        )
        every { brevmottakerVedtaksbrevRepository.findByBehandlingId(saksbehandling.id) } returns listOf(
            brevmottakerDuplikat,
            brevmottakerDuplikat,
        )

        val brevmottaker = mockk<BrevmottakerVedtaksbrev>()
        every { journalpostService.opprettJournalpost(any()) } returns ArkiverDokumentResponse(
            journalpostId = "mocket JournalpostId",
            ferdigstilt = true,
            dokumenter = null,
        )
        every { brevmottakerVedtaksbrevRepository.update(any()) } returns brevmottaker

        journalførVedtaksbrevTask.doTask(task)

        verify(exactly = 2) { journalpostService.opprettJournalpost(any()) }
        verify(exactly = 2) { brevmottakerVedtaksbrevRepository.update(any()) }
    }

    @Test
    internal fun `kaster feil hvis dokument fra arkiv ikke er ferdigstilt`() {
        val brevmottaker = mockk<BrevmottakerVedtaksbrev>()
        every { journalpostService.opprettJournalpost(any()) } returns ArkiverDokumentResponse(
            journalpostId = "mocket JournalpostId",
            ferdigstilt = false,
            dokumenter = null,
        )
        every { brevmottakerVedtaksbrevRepository.update(any()) } returns brevmottaker

        Assertions.assertThatThrownBy {
            journalførVedtaksbrevTask.doTask(task)
        }.isInstanceOf(Feil::class.java).message()
            .isEqualTo("Journalposten ble ikke ferdigstilt og kan derfor ikke distribueres")
    }

    @Test
    internal fun `Det skal ikke opprettes ny journalpost for brevmottakere som allerede har journalpostId`() {
        val arkiverDokumentRequestSlot = slot<ArkiverDokumentRequest>()

        val brevmottakerUUID = UUID.randomUUID()

        val brevmottakerUtenJournalføringsId = BrevmottakerVedtaksbrev(
            id = brevmottakerUUID,
            behandlingId = saksbehandling.id,
            mottaker = mottakerPerson(ident = saksbehandling.ident),
        )

        val brevmottakerMedJournalføringsId = BrevmottakerVedtaksbrev(
            behandlingId = saksbehandling.id,
            mottaker = mottakerPerson(ident = saksbehandling.ident),
            journalpostId = "eksisterende journalpost id",
        )
        every { brevmottakerVedtaksbrevRepository.findByBehandlingId(saksbehandling.id) } returns listOf(
            brevmottakerUtenJournalføringsId,
            brevmottakerMedJournalføringsId,
        )

        val brevmottaker = mockk<BrevmottakerVedtaksbrev>()
        every { journalpostService.opprettJournalpost(any()) } returns ArkiverDokumentResponse(
            journalpostId = "mocket JournalpostId",
            ferdigstilt = true,
            dokumenter = null,
        )
        every { brevmottakerVedtaksbrevRepository.update(any()) } returns brevmottaker

        journalførVedtaksbrevTask.doTask(task)

        verify(exactly = 1) { journalpostService.opprettJournalpost(capture(arkiverDokumentRequestSlot)) }
        verify(exactly = 1) { brevmottakerVedtaksbrevRepository.update(any()) }

        assertThat(arkiverDokumentRequestSlot.captured.avsenderMottaker?.id!!.equals(brevmottakerUUID))
    }
}
