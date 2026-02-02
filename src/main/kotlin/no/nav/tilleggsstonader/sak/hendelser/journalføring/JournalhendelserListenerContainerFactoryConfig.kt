package no.nav.tilleggsstonader.sak.hendelser.journalføring

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tilleggsstonader.libs.kafka.KafkaErrorHandler
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import java.time.Duration

@Configuration
@Profile("!local & !integrasjonstest")
class JournalhendelserListenerContainerFactoryConfig {
    @Bean
    fun journalhendelserListenerContainerFactory(
        properties: KafkaProperties,
        kafkaErrorHandler: KafkaErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<Long, JournalfoeringHendelseRecord> {
        val consumerProperties =
            properties.buildConsumerProperties().apply {
                this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 1
                this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
                this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
                this[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = true
                this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
                this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaAvroDeserializer::class.java
            }

        return ConcurrentKafkaListenerContainerFactory<Long, JournalfoeringHendelseRecord>().apply {
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
            containerProperties.setAuthExceptionRetryInterval(Duration.ofSeconds(2))
            setCommonErrorHandler(kafkaErrorHandler)
            setConsumerFactory(DefaultKafkaConsumerFactory(consumerProperties))
        }
    }
}
