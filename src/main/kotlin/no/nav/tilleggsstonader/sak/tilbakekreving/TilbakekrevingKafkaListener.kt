package no.nav.tilleggsstonader.sak.tilbakekreving

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.tilbakekreving.håndter.TilbakekrevingHendelseHåndterer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
@Profile("!local & !integrasjonstest")
class TilbakekrevingKafkaListener(
    private val tilbakekrevingHendelseHåndterere: Collection<TilbakekrevingHendelseHåndterer>,
) {
    companion object {
        const val TILBAKEKREVING_TOPIC = "tilbake.privat-tilbakekreving-tilleggsstonad"
        const val HENDELSESTYPE_FAGSYSTEMINFO_BEHOV = "fagsysteminfo_behov"

        private val logger = LoggerFactory.getLogger(TilbakekrevingKafkaListener::class.java)
    }

    @KafkaListener(
        groupId = "tilleggsstonader-sak",
        topics = [TILBAKEKREVING_TOPIC],
        containerFactory = "tilbakekrevingKravgrunnlagOppslagListenerContainerFactory",
    )
    fun listen(
        consumerRecord: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        val payload = objectMapper.readTree(consumerRecord.value())
        val hendelsestype = payload.get("hendelsestype")?.asText()

        val håndterer =
            tilbakekrevingHendelseHåndterere.firstOrNull { it.håndtererHendelsetype() == hendelsestype }

        if (håndterer != null) {
            håndterer.håndter(consumerRecord.key(), payload)
        } else {
            logger.info("Ignorerer tilbakekreving-hendelse av type $hendelsestype")
        }

        ack.acknowledge()
    }
}
