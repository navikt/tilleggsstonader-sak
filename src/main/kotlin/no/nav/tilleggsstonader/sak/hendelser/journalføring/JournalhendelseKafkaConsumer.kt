package no.nav.tilleggsstonader.sak.hendelser.journalføring

import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

@Service
@Profile("!local & !integrasjonstest")
class JournalhendelseKafkaConsumer(
    private val journalhendelseKafkaListener: JournalhendelseKafkaListener,
) {
    @KafkaListener(
        groupId = "tilleggsstonader-sak",
        topics = ["\${topics.journalhendelser}"],
        containerFactory = "journalhendelserListenerContainerFactory",
    )
    fun listen(
        consumerRecord: ConsumerRecord<String, JournalfoeringHendelseRecord>,
        ack: Acknowledgment,
    ) {
        journalhendelseKafkaListener.listen(consumerRecord, ack)
    }
}
