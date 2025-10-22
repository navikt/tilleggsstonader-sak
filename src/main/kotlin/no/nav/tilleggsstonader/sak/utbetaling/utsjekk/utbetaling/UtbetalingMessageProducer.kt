package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider
import no.nav.tilleggsstonader.libs.log.logger
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UtbetalingMessageProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value($$"${topics.utbetaling}") private val topic: String,
) {
    fun sendUtbetalinger(
        iverksettingId: UUID,
        utbetaling: List<UtbetalingRecord>,
    ) {
        val producerRecords =
            utbetaling.map { utbetalingRecord ->
                ProducerRecord(
                    topic,
                    iverksettingId.toString(),
                    ObjectMapperProvider.objectMapper.writeValueAsString(utbetalingRecord),
                )
            }
        // Bruker iverksettingId som key for å si at helved skal se på utbetalingene samlet (transaksjonsid hos helved).
        // Vi mottar status på transaksjonsId
        // .get() til slutt for å vente på at alle er sendt
        producerRecords
            .map { kafkaTemplate.send(it) }
            .forEach { it.get() }
    }
}
