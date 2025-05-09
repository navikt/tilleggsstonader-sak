package no.nav.tilleggsstonader.sak.hendelser.personhendelse

import no.nav.person.pdl.leesah.Personhendelse
import no.nav.tilleggsstonader.sak.hendelser.personhendelse.dødsfall.DødsfallHendelse
import no.nav.tilleggsstonader.sak.hendelser.personhendelse.dødsfall.DødsfallHåndterer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

@Service
@Profile("!local & !integrasjonstest & !prod")
class PersonhendelseKafkaListener(
    private val dødsfallHåndterer: DødsfallHåndterer,
) {
    @KafkaListener(
        id = "tilleggsstonader-sak",
        topics = ["\${topics.leesah}"],
        containerFactory = "personhendelserListenerContainerFactory",
    )
    fun listen(
        consumerRecords: List<ConsumerRecord<String, Personhendelse>>,
        acknowledgment: Acknowledgment,
    ) {
        val dødsfallhendelser =
            consumerRecords
                .map { it.value() }
                .filter { it.erDødsfall() }
                .map { it.tilDødsfallDomene() }

        if (dødsfallhendelser.isNotEmpty()) {
            dødsfallHåndterer.håndterDødsfall(dødsfallhendelser)
        }
        acknowledgment.acknowledge()
    }
}

private fun Personhendelse.erDødsfall() = doedsfall != null

private fun Personhendelse.tilDødsfallDomene() = DødsfallHendelse(this.doedsfall.doedsdato, this.personidenter.toSet())
