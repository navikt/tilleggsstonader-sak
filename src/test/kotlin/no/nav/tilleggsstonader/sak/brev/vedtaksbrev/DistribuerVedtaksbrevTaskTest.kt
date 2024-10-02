package no.nav.tilleggsstonader.sak.brev.vedtaksbrev

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerUtil.mottakerPerson
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.MottakerRolle
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DistribuerVedtaksbrevTaskTest {

    val brevmottakerVedtaksbrevRepository = mockk<BrevmottakerVedtaksbrevRepository>()
    val journalpostClient = mockk<JournalpostClient>()
    val stegService = mockk<StegService>()
    val brevSteg = mockk<BrevSteg>()

    val saksbehandling = saksbehandling()
    val distribuerVedtaksbrevTask =
        DistribuerVedtaksbrevTask(brevmottakerVedtaksbrevRepository, journalpostClient, stegService, brevSteg, TransactionHandler())
    val task = Task(type = DistribuerVedtaksbrevTask.TYPE, payload = saksbehandling.id.toString())

    @BeforeEach
    fun setUp() {
        every { stegService.håndterSteg(any<BehandlingId>(), brevSteg) } returns mockk()
    }

    @Test
    fun `skal distribuere brev til alle brevmottakere`() {
        val distribuerrequestSlots = mutableListOf<DistribuerJournalpostRequest>()
        val brevmottakereSlots = mutableListOf<BrevmottakerVedtaksbrev>()

        val journalpostIdA = "journalpostIdA"
        val journalpostIdB = "journalpostIdB"
        every { brevmottakerVedtaksbrevRepository.findByBehandlingId(saksbehandling.id) } returns listOf(
            BrevmottakerVedtaksbrev(
                behandlingId = saksbehandling.id,
                mottaker = mottakerPerson(ident = saksbehandling.ident),
                journalpostId = journalpostIdA,
                bestillingId = null,
            ),
            BrevmottakerVedtaksbrev(
                behandlingId = saksbehandling.id,
                mottaker = mottakerPerson(
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

        val journalpostIdA = "journalpostIdA"
        val journalpostIdB = "journalpostIdB"
        every { brevmottakerVedtaksbrevRepository.findByBehandlingId(saksbehandling.id) } returns listOf(
            BrevmottakerVedtaksbrev(
                behandlingId = saksbehandling.id,
                mottaker = mottakerPerson(ident = saksbehandling.ident),
                journalpostId = journalpostIdA,
                bestillingId = "alleredeDistribuertId",
            ),
            BrevmottakerVedtaksbrev(
                behandlingId = saksbehandling.id,
                mottaker = mottakerPerson(
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
}
