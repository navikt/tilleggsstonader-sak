package no.nav.tilleggsstonader.sak.hendelser.oppgave

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Profile("!local & !integrasjonstest & !prod")
class OppgavehendelseKafkaListener(
    private val oppgavehendelseHåndterer: OppgavehendelseHåndterer,
) {
    @KafkaListener(
        groupId = "tilleggsstonader-sak",
        topics = ["\${topics.oppgavehendelser}"],
        containerFactory = "oppgavehendelserListenerContainerFactory",
    )
    @Transactional
    fun listen(
        consumerRecords: List<ConsumerRecord<String, OppgavehendelseRecord>>,
        acknowledgment: Acknowledgment,
    ) {
        oppgavehendelseHåndterer.behandleOppgavehendelser(consumerRecords.map { it.value() })
        acknowledgment.acknowledge()
    }
}
