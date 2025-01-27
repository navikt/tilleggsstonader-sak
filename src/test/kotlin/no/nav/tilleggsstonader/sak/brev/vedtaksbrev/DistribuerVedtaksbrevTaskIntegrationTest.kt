package no.nav.tilleggsstonader.sak.brev.vedtaksbrev

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.prosessering.internal.TaskWorker
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerTestUtil.mottakerPerson
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.MottakerRolle
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.JournalpostClientConfig
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class DistribuerVedtaksbrevTaskIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var taskWorker: TaskWorker

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var brevmottakerVedtaksbrevRepository: BrevmottakerVedtaksbrevRepository

    @Test
    fun `bestillingId skal lagres selv om task feiler`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val task = taskService.save(Task(type = DistribuerVedtaksbrevTask.TYPE, behandling.id.toString()))
        brevmottakerVedtaksbrevRepository.insertAll(
            listOf(
                BrevmottakerVedtaksbrev(
                    behandlingId = behandling.id,
                    mottaker = mottakerPerson(ident = "ident"),
                    journalpostId = "journalpostIdA",
                    bestillingId = null,
                ),
                BrevmottakerVedtaksbrev(
                    behandlingId = behandling.id,
                    mottaker =
                        mottakerPerson(
                            mottakerRolle = MottakerRolle.VERGE,
                            ident = "identAnnenMottaker",
                        ),
                    journalpostId = JournalpostClientConfig.journalpostIdMedFeil,
                    bestillingId = null,
                ),
            ),
        )

        utførTask(task)

        val resultat = brevmottakerVedtaksbrevRepository.findByBehandlingId(behandling.id)

        assertThat(resultat.size).isEqualTo(2)
        assertThat(resultat[0].bestillingId).isEqualTo("bestillingId")
        assertThat(resultat[1].bestillingId).isNull()
    }

    private fun utførTask(task: Task) {
        try {
            taskWorker.markerPlukket(task.id)
            taskWorker.doActualWork(task.id)
        } catch (_: Exception) {
        }
    }
}
