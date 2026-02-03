package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UtbetalingMessageProducer(
    @Value($$"${topics.utbetaling}") private val topic: String,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val iverksettingLoggRepository: IverksettingLoggRepository,
) {
    fun sendUtbetalinger(
        iverksettingId: UUID,
        utbetaling: IverksettingDto,
    ) {
        val utbetalingJson = jsonMapper.writeValueAsString(utbetaling)
        iverksettingLoggRepository.insert(
            IverksettingLogg(
                iverksettingId = iverksettingId,
                utbetalingJson = JsonWrapper(utbetalingJson),
            ),
        )
        val producerRecord =
            ProducerRecord(
                topic,
                iverksettingId.toString(),
                utbetalingJson,
            )

        // Bruker iverksettingId som key for å si at helved skal se på utbetalingene samlet (transaksjonsid hos helved).
        // Vi mottar status på transaksjonsId
        // .get() til slutt for å vente på at alle er sendt
        kafkaTemplate.send(producerRecord).get()
    }
}
