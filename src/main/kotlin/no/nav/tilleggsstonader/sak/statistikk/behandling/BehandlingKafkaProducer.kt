package no.nav.tilleggsstonader.sak.statistikk.behandling

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.saksstatistikk.BehandlingDVH
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class BehandlingKafkaProducer(
    @Value("\${DVH_BEHANDLING_TOPIC}") private val topic: String,
    val kafkaTemplate: KafkaTemplate<String, String>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun sendBehandling(
        behandlingDVH: BehandlingDVH,
        stønadstype: Stønadstype,
    ) {
        logger.info("Sending to Kafka topic: $topic")
        secureLogger.debug("Sending to Kafka topic: {}\nBehandlingstatistikk: {}", topic, behandlingDVH)
        runCatching {
            sendMedStønadstypeIHeader(
                topic,
                stønadstype,
                behandlingDVH.behandlingId,
                jsonMapper.writeValueAsString(behandlingDVH),
            )
            logger.info(
                "Behandlingstatistikk for behandling=${behandlingDVH.behandlingId} " +
                    "behandlingStatus=${behandlingDVH.behandlingStatus} sent til Kafka",
            )
        }.onFailure {
            val errorMessage = "Could not send behandling to Kafka. Check secure logs for more information."
            logger.error(errorMessage)
            secureLogger.error("Could not send behandling to Kafka", it)
            throw RuntimeException(errorMessage)
        }
    }

    private fun sendMedStønadstypeIHeader(
        topic: String,
        stønadstype: Stønadstype,
        key: String,
        payload: String,
    ) {
        val record = ProducerRecord(topic, key, payload)
        record.headers().add(RecordHeader("stønadstype", stønadstype.name.toByteArray()))
        kafkaTemplate.send(record).get()
    }
}
