package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jboss.logging.MDC
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

const val IVERKSETTING_ID = "iverksettingId"

@Service
@Profile("!local & !integrasjonstest")
class UtbetalingStatusKafkaListener(
    private val utbetalingStatusHåndterer: UtbetalingStatusHåndterer,
) {
    @KafkaListener(
        groupId = "tilleggsstonader-sak",
        topics = [$$"${topics.utbetaling-status}"],
        containerFactory = "utbetalingStatusListenerContainerFactory",
    )
    @Transactional
    fun listen(
        consumerRecord: ConsumerRecord<String, UtbetalingStatusRecord>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            val iverksettingId = consumerRecord.key()
            MDC.put(IVERKSETTING_ID, iverksettingId)
            utbetalingStatusHåndterer.behandleStatusoppdatering(
                iverksettingId = iverksettingId,
                melding = consumerRecord.value(),
                utbetalingGjelderFagsystem =
                    consumerRecord
                        .headers()
                        .firstOrNull { it.key() == "fagsystem" }
                        ?.value()
                        ?.let { String(it) } ?: "Ukjent",
            )
            acknowledgment.acknowledge()
        } finally {
            MDC.remove(IVERKSETTING_ID)
        }
    }
}
