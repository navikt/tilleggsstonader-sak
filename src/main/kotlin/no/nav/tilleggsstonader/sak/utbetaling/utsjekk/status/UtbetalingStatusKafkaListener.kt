package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
        utbetalingStatusHåndterer.behandleStatusoppdatering(iverksettingId = consumerRecord.key(), melding = consumerRecord.value())
        acknowledgment.acknowledge()
    }
}
