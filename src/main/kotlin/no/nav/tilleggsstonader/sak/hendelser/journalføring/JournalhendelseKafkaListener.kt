package no.nav.tilleggsstonader.sak.hendelser.journalføring

import no.nav.familie.prosessering.internal.TaskService
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.libs.log.IdUtils
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.libs.log.mdc.MDCConstants
import no.nav.tilleggsstonader.sak.hendelser.Hendelse
import no.nav.tilleggsstonader.sak.hendelser.HendelseRepository
import no.nav.tilleggsstonader.sak.hendelser.TypeHendelse
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

/**
 * Lytting på journalhendelser fra dokarkiv
 * https://github.com/navikt/teamdokumenthandtering-avro-schemas
 * https://confluence.adeo.no/pages/viewpage.action?pageId=432217859
 * https://confluence.adeo.no/display/BOA/Joarkhendelser
 */
@Service
@Profile("!local & !integrasjonstest")
class JournalhendelseKafkaListener(
    private val hendelseRepository: HendelseRepository,
    private val transactionHandler: TransactionHandler,
    private val taskService: TaskService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        groupId = "tilleggsstonader-sak",
        topics = ["\${topics.journalhendelser}"],
        containerFactory = "journalhendelserListenerContainerFactory",
    )
    fun listen(
        consumerRecord: ConsumerRecord<String, JournalfoeringHendelseRecord>,
        ack: Acknowledgment,
    ) {
        try {
            val hendelseRecord = consumerRecord.value()
            MDC.put(MDCConstants.MDC_CALL_ID, getCallId(hendelseRecord))
            prosesserHendelse(hendelseRecord)
            ack.acknowledge()
        } catch (e: Exception) {
            logger.error("Feil ved prosessering av journalhendelser. Se secure logg for detaljer.")
            secureLogger.error("Feil ved prosessering av journalhendelser.", e)
            throw e
        } finally {
            MDC.clear()
        }
    }

    private fun prosesserHendelse(hendelseRecord: JournalfoeringHendelseRecord) {
        if (!hendelseRecord.skalProsesseres()) {
            return
        }
        val hendelseId = hendelseRecord.hendelsesId.takeIf { it.isNotBlank() } ?: error("Mangler hendelseId")
        if (!hendelseRepository.existsByTypeAndId(TypeHendelse.JOURNALPOST, hendelseId)) {
            transactionHandler.runInNewTransaction {
                taskService.save(JournalhendelseKafkaHåndtererTask.opprettTask(hendelseRecord))
                val metadata = mapOf("journalpostId" to hendelseRecord.journalpostId)
                val hendelse = Hendelse(TypeHendelse.JOURNALPOST, id = hendelseId, metadata = metadata)
                hendelseRepository.insert(hendelse)
            }
        }
    }

    private fun JournalfoeringHendelseRecord.skalProsesseres(): Boolean =
        Tema.gjelderTemaTilleggsstønader(temaNytt) && erHendelsetypeGyldig()

    fun JournalfoeringHendelseRecord.erHendelsetypeGyldig(): Boolean =
        JournalpostHendelseType.valueOf(hendelsesType) == JournalpostHendelseType.JournalpostMottatt

    private fun getCallId(hendelseRecord: JournalfoeringHendelseRecord) =
        hendelseRecord.kanalReferanseId?.takeIf { it.isNotBlank() } ?: IdUtils.generateId()
}

private enum class JournalpostHendelseType {
    JournalpostMottatt,
    TemaEndret,
    EndeligJournalført,
    JournalpostUtgått,
}

fun main() {
    println(JournalfoeringHendelseRecord())
}
