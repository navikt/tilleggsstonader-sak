package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.KafkaProducerService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class VedtakKafkaProducer(private val kafkaProducerService: KafkaProducerService) {
    @Value("\${TILLEGGSTONADER_VEDTAK_TOPIC}")
    lateinit var topic: String

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun sendVedtak(behandlingId: Long, stønadstype: Stønadstype, vedtakStatistikk: String) {
        logger.info("Sending to Kafka topic: {}", topic)
        secureLogger.debug("Sending to Kafka topic: {}\nVedtakStatistikk: {}", topic, vedtakStatistikk)

        runCatching {
            kafkaProducerService.sendMedStønadstypeIHeader(topic, stønadstype, behandlingId.toString(), vedtakStatistikk)
            logger.info("Vedtakstatistikk for behandling=$behandlingId sent til Kafka")
        }.onFailure {
            val errorMessage = "Could not send vedtak to Kafka. Check secure logs for more information."
            logger.error(errorMessage)
            secureLogger.error("Could not send vedtak to Kafka", it)
            throw RuntimeException(errorMessage)
        }
    }
}
