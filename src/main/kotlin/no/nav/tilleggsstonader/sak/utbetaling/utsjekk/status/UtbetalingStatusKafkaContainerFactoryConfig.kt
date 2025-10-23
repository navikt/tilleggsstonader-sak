package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status

import no.nav.tilleggsstonader.libs.kafka.KafkaErrorHandler
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.ssl.SslBundles
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonDeserializer
import java.time.Duration

@Configuration
@Profile("!local & !integrasjonstestc & !prod")
class UtbetalingStatusKafkaContainerFactoryConfig {
    @Bean
    fun utbetalingStatusListenerContainerFactory(
        properties: KafkaProperties,
        kafkaErrorHandler: KafkaErrorHandler,
        sslBundles: ObjectProvider<SslBundles>,
    ): ConcurrentKafkaListenerContainerFactory<String, UtbetalingStatusRecord> {
        val consumerProperties =
            properties.buildConsumerProperties(sslBundles.getIfAvailable()).apply {
                this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 1
                this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
                this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
                this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
                this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
                this[JsonDeserializer.TRUSTED_PACKAGES] = "no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status"
                this[JsonDeserializer.VALUE_DEFAULT_TYPE] = UtbetalingStatusRecord::class.java.name
            }

        return ConcurrentKafkaListenerContainerFactory<String, UtbetalingStatusRecord>().apply {
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
            containerProperties.setAuthExceptionRetryInterval(Duration.ofSeconds(2))
            setCommonErrorHandler(kafkaErrorHandler)
            consumerFactory = DefaultKafkaConsumerFactory(consumerProperties)
        }
    }
}
