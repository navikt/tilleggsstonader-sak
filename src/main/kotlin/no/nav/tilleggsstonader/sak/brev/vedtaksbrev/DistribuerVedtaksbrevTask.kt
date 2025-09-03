package no.nav.tilleggsstonader.sak.brev.vedtaksbrev

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.brev.vedtaksbrev.DistribuerVedtaksbrevService.ResultatBrevutsendelse
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.util.stoppTaskOgRekjørSenere
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

/**
 * Distribuering av vedtaksbrev skjer asynkront etter at journalposten er opprettet.
 * Det er ikke en del av behandlingsflyten sånn at en distribuering ikke skal stoppe behandlingen.
 *
 * I tilfelle en person er død, vil distribueringen feile med 410 Gone, og denne tasken vil avbrytes og kjøres på nytt senere.
 */
@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerVedtaksbrevTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 3 * 60L,
    beskrivelse = "Distribuerer vedtaksbrev etter journalføring",
)
class DistribuerVedtaksbrevTask(
    private val brevmottakerVedtaksbrevRepository: BrevmottakerVedtaksbrevRepository,
    private val distribuerVedtaksbrevService: DistribuerVedtaksbrevService,
    private val taskService: TaskService,
) : AsyncTaskStep {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)

        brevmottakerVedtaksbrevRepository
            .findByBehandlingId(behandlingId)
            .ifEmpty { throw Feil("Ingen brevmottakere funnet") }
            .filter { it.harIkkeFåttBrevet() }
            .map { distribuerVedtaksbrevService.distribuerVedtaksbrev(mottaker = it) }
            .håndterRekjøringSenereHvisMottakerErDød(task)
    }

    private fun List<ResultatBrevutsendelse>.håndterRekjøringSenereHvisMottakerErDød(task: Task) {
        filterIsInstance<ResultatBrevutsendelse.FeiletFordiMottakerErDødOgManglerAdresse>()
            .firstOrNull()
            ?.let { taskService.stoppTaskOgRekjørSenere(task, årsak = "Mottaker er død", melding = it.feilmelding) }
    }

    companion object {
        fun opprettTask(behandlingId: BehandlingId): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties =
                    Properties().apply {
                        setProperty("behandlingId", behandlingId.toString())
                    },
            )

        const val TYPE = "distribuerVedtaksbrev"
    }
}
