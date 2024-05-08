package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import java.util.concurrent.CompletableFuture

@Configuration
@Profile("mock-kafka")
class KafkaTestConfig {

    @Bean
    @Primary
    fun kafkaTemplate(): KafkaTemplate<String, String> {
        val mock = mockk<KafkaTemplate<String, String>>(relaxed = true)
        every { mock.send(any<ProducerRecord<String, String>>()) } returns CompletableFuture.completedFuture(mockk())
        return mock
    }
}
