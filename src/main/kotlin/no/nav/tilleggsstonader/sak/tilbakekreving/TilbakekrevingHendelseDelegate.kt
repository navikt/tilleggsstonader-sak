package no.nav.tilleggsstonader.sak.tilbakekreving

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.tilbakekreving.håndter.TilbakekrevingHendelseHåndterer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TilbakekrevingHendelseDelegate(
    private val tilbakekrevingHendelseHåndterere: Collection<TilbakekrevingHendelseHåndterer>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun håndter(consumerRecord: ConsumerRecord<String, String>) {
        val payload = objectMapper.readTree(consumerRecord.value())
        val hendelsestype = payload.get("hendelsestype")?.asText()

        val håndterer =
            tilbakekrevingHendelseHåndterere.firstOrNull { it.håndtererHendelsetype() == hendelsestype }

        if (håndterer != null) {
            håndterer.håndter(consumerRecord.key(), payload)
        } else {
            logger.info("Ignorerer tilbakekreving-hendelse av type $hendelsestype")
        }
    }
}
