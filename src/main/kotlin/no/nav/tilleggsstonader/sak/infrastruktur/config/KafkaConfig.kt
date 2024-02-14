package no.nav.tilleggsstonader.sak.infrastruktur.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.support.LoggingProducerListener

@EnableKafka
@Configuration
class KafkaConfig {

    @Bean
    fun loggingProducerListener() = LoggingProducerListener<Any, Any>().apply {
        setIncludeContents(false)
    }
}
