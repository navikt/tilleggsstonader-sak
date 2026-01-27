package no.nav.tilleggsstonader.sak.hendelser.journalføring

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import org.jboss.logging.MDC
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

const val JOURNALPOST_ID = "journalpostId"

@Service
@TaskStepBeskrivelse(
    taskStepType = JournalhendelseKafkaHåndtererTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 60L,
    beskrivelse = "Håndterer hendelse fra journalposthendelser, journalfører søknad",
)
class JournalhendelseKafkaHåndtererTask(
    private val journalhendelseKafkaHåndtererService: JournalhendelseKafkaHåndtererService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val hendelse = objectMapper.readValue<JournalhendelseTaskData>(task.payload)
        try {
            MDC.put(JOURNALPOST_ID, hendelse.journalpostId)
            logger.info("Behandler mottatt journalpost {}", hendelse.journalpostId)
            journalhendelseKafkaHåndtererService.behandleJournalhendelse(journalpostId = hendelse.journalpostId)
        } finally {
            MDC.remove(JOURNALPOST_ID)
        }
    }

    private data class JournalhendelseTaskData(
        val journalpostId: String,
        val hendelseId: String,
    )

    companion object {
        private const val TYPE = "journalhendelseTask"

        fun opprettTask(hendelse: JournalfoeringHendelseRecord): Task {
            val taskData =
                JournalhendelseTaskData(
                    journalpostId = hendelse.journalpostId.toString(),
                    hendelseId = hendelse.hendelsesId,
                )
            return Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(taskData),
            )
        }
    }
}
