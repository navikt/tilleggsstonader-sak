package no.nav.tilleggsstonader.sak.statistikk.behandling

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.tilleggsstonader.sak.infrastruktur.KafkaProducerService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class BehandlingKafkaProducer(
    private val kafkaProducerService: KafkaProducerService,
    @Value("\${DVH_BEHANDLING_TOPIC}") private val topic: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun sendBehandling(behandlingDVH: BehandlingDVH) {
        logger.info("Sending to Kafka topic: ${topic}")
        secureLogger.debug("Sending to Kafka topic: {}\nBehandlingstatistikk: {}", topic, behandlingDVH)
        runCatching {
            kafkaProducerService.send(topic, behandlingDVH.behandlingId, behandlingDVH.toJson())
            logger.info(
                "Behandlingstatistikk for behandling=${behandlingDVH.behandlingId} " + "behandlingStatus=${behandlingDVH.behandlingStatus} sent til Kafka",
            )
        }.onFailure {
            val errorMessage = "Could not send behandling to Kafka. Check secure logs for more information."
            logger.error(errorMessage)
            secureLogger.error("Could not send behandling to Kafka", it)
            throw RuntimeException(errorMessage)
        }
    }

    private fun Any.toJson(): String = objectMapper.writeValueAsString(this)
}
