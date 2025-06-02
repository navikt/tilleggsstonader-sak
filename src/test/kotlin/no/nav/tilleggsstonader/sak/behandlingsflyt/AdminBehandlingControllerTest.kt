package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.prosessering.internal.TaskWorker
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerTestUtil.mottakerPerson
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.statistikk.task.BehandlingsstatistikkTask
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.test.context.transaction.TestTransaction
import java.time.LocalDateTime

class AdminBehandlingControllerTest : IntegrationTest() {
    @Autowired
    lateinit var adminLæremidlerVedtakController: AdminBehandlingController

    @Autowired
    lateinit var taskService: TaskService

    @Autowired
    lateinit var taskWorker: TaskWorker

    @Autowired
    lateinit var vedtakRepository: VedtakRepository

    @Autowired
    lateinit var brevmottakerVedtaksbrevRepository: BrevmottakerVedtaksbrevRepository

    private val behandling = behandling()

    @Test
    fun `ferdigstiller behandling`() {
        testoppsettService.opprettBehandlingMedFagsak(
            behandling.copy(
                status = BehandlingStatus.IVERKSETTER_VEDTAK,
                steg = StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV,
                resultat = BehandlingResultat.INNVILGET,
                vedtakstidspunkt = LocalDateTime.now(),
            ),
        )
        vedtakRepository.insert(innvilgetVedtak(behandling.id))

        val brevmottakerVedtaksbrev =
            BrevmottakerVedtaksbrev(
                behandlingId = behandling.id,
                mottaker = mottakerPerson(ident = "ident"),
                journalpostId = "journalpostId",
            )
        brevmottakerVedtaksbrevRepository.insert(brevmottakerVedtaksbrev)

        adminLæremidlerVedtakController.ferdigstill(behandling.id)
        kjørTasks()
        newTransaction()
        val oppdatertBehandling = testoppsettService.hentBehandling(behandling.id)
        assertThat(oppdatertBehandling.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
    }

    private fun kjørTasks() {
        newTransaction()
        logger.info("Kjører tasks")
        taskService
            .finnAlleTasksKlareForProsessering(Pageable.unpaged())
            .filterNot { it.type == BehandlingsstatistikkTask.TYPE } // Tester ikke statistikkutsendelse her
            .forEach {
                taskWorker.markerPlukket(it.id)
                logger.info("Kjører task ${it.id} type=${it.type}")
                taskWorker.doActualWork(it.id)
            }
        logger.info("Tasks kjørt OK")
    }

    private fun newTransaction() {
        if (TestTransaction.isActive()) {
            TestTransaction.flagForCommit() // need this, otherwise the next line does a rollback
            TestTransaction.end()
            TestTransaction.start()
        }
    }
}
