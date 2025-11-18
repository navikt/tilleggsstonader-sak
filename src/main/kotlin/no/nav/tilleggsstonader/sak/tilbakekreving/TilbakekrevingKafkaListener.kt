package no.nav.tilleggsstonader.sak.tilbakekreving

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
@Profile("!local & !integrasjonstest")
class TilbakekrevingKafkaListener(
    private val tilbakekrevingHendelseDelegate: TilbakekrevingHendelseDelegate,
) {
    @KafkaListener(
        groupId = "tilleggsstonader-sak",
        topics = [TILBAKEKREVING_TOPIC],
        containerFactory = "tilbakekrevingKravgrunnlagOppslagListenerContainerFactory",
    )
    fun listen(
        consumerRecord: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        tilbakekrevingHendelseDelegate.håndter(consumerRecord)
        ack.acknowledge()
    }
}

const val TILBAKEKREVING_TOPIC = "tilbake.privat-tilbakekreving-tilleggsstonad"
