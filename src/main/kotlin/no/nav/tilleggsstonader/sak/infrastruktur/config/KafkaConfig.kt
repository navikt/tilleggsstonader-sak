package no.nav.tilleggsstonader.sak.infrastruktur.config

import no.nav.tilleggsstonader.libs.kafka.KafkaErrorHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.support.LoggingProducerListener

@EnableKafka
@Configuration
@Import(
    KafkaErrorHandler::class,
)
class KafkaConfig {
    @Bean
    fun loggingProducerListener() =
        LoggingProducerListener<Any, Any>().apply {
            setIncludeContents(false)
        }
}
