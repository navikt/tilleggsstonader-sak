package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider
import no.nav.tilleggsstonader.libs.log.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class UtbetalingMessageProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value($$"${topics.utbetaling}") private val topic: String,
) {
    fun sendUtbetaling(utbetaling: UtbetalingRecord) {
        kafkaTemplate
            .send(topic, utbetaling.behandlingId, ObjectMapperProvider.objectMapper.writeValueAsString(utbetaling))
            .whenComplete { result, ex ->
                if (ex == null) {
                    logger.info("Sente utbetaling på topic=$topic med offset=${result.recordMetadata.offset()}")
                } else {
                    logger.info(
                        "Sending på topic=$topic feilet: ${ex.message}",
                    )
                }
            }
    }
}
