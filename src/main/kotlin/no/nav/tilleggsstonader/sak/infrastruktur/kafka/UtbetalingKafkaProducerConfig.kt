package no.nav.tilleggsstonader.sak.infrastruktur.kafka

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.ssl.SslBundles
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate

@Configuration
@Profile("!local & !integrasjonstest")
class UtbetalingKafkaProducerConfig {
    @Bean
    fun utsjekkKafkaTemplate(
        properties: KafkaProperties,
        sslBundles: ObjectProvider<SslBundles>,
    ): KafkaTemplate<String, String> {
        val producerProperties =
            properties.buildProducerProperties(sslBundles.getIfAvailable()).apply {
                this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
                this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
                this[ProducerConfig.ACKS_CONFIG] = "all"
                this[ProducerConfig.RETRIES_CONFIG] = 3
            }

        return KafkaTemplate(DefaultKafkaProducerFactory(producerProperties))
    }
}
