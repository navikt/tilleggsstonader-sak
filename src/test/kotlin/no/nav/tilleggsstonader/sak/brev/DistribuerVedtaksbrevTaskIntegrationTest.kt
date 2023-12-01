package no.nav.tilleggsstonader.sak.brev

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.prosessering.internal.TaskWorker
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.brev.brevmottaker.Brevmottaker
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerRolle
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerType
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
    private lateinit var brevmottakerRepository: BrevmottakerRepository

    @Test
    fun `bestillingId skal lagres selv om task feiler`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val task = taskService.save(Task(type = DistribuerVedtaksbrevTask.TYPE, behandling.id.toString()))
        brevmottakerRepository.insertAll(
            listOf(
                Brevmottaker(
                    behandlingId = behandling.id,
                    mottakerRolle = MottakerRolle.BRUKER,
                    mottakerType = MottakerType.PERSON,
                    ident = "ident",
                    journalpostId = "journalpostIdA",
                    bestillingId = null,
                ),
                Brevmottaker(
                    behandlingId = behandling.id,
                    mottakerRolle = MottakerRolle.VERGE,
                    mottakerType = MottakerType.PERSON,
                    ident = "identAnnenMottaker",
                    journalpostId = JournalpostClientConfig.journalpostIdMedFeil,
                    bestillingId = null,
                ),
            ),
        )

        utførTask(task)

        val resultat = brevmottakerRepository.findByBehandlingId(behandling.id)

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
