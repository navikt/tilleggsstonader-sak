package no.nav.tilleggsstonader.sak.hendelser.personhendelse

import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.tilleggsstonader.sak.hendelser.personhendelse.dødsfall.DødsfallHendelse
import no.nav.tilleggsstonader.sak.hendelser.personhendelse.dødsfall.DødsfallHåndterer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.MDC
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

@Service
@Profile("!local & !integrasjonstest & !prod")
class PersonhendelseKafkaListener(
    private val dødsfallHåndterer: DødsfallHåndterer,
) {
    companion object {
        private const val MDC_HENDELSE_ID_KEY = "hendelseId"
    }

    @KafkaListener(
        id = "tilleggsstonader-sak",
        topics = ["\${topics.pdl-personhendelser}"],
        containerFactory = "personhendelserListenerContainerFactory",
    )
    fun listen(
        consumerRecords: List<ConsumerRecord<String, Personhendelse>>,
        acknowledgment: Acknowledgment,
    ) {
        consumerRecords
            .map { it.value() }
            .filter { it.erDødsfall() }
            .map { it.tilDødsfallDomene() }
            .forEach {
                try {
                    MDC.put(MDC_HENDELSE_ID_KEY, it.hendelseId)
                    dødsfallHåndterer.håndter(it)
                } finally {
                    MDC.remove(MDC_HENDELSE_ID_KEY)
                }
            }

        acknowledgment.acknowledge()
    }
}

private fun Personhendelse.erDødsfall() = doedsfall != null

private fun Personhendelse.tilDødsfallDomene() =
    DødsfallHendelse(
        if (this.erAnnullering()) this.tidligereHendelseId else this.hendelseId,
        this.doedsfall.doedsdato,
        this.personidenter.toSet(),
        erAnnullering(),
    )

private fun Personhendelse.erAnnullering() = this.endringstype == Endringstype.ANNULLERT
