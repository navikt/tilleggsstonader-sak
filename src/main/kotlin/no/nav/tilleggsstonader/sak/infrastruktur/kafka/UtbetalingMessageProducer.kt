package no.nav.tilleggsstonader.sak.infrastruktur.kafka

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.libs.log.logger
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.UtbetalingRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class UtbetalingMessageProducer(
    private val utsjekkKafkaTemplate: KafkaTemplate<String, String>,
    @Value($$"${topics.utbetaling}") private val topic: String,
) {
    fun sendUtbetaling(utbetaling: UtbetalingRecord) {
        utsjekkKafkaTemplate
            .send(topic, utbetaling.behandlingId, objectMapper.writeValueAsString(utbetaling))
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
