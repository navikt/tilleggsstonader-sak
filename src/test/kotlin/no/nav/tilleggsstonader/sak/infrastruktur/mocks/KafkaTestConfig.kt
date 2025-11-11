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
        resetMock(mock)
        return mock
    }

    companion object {
        private val sendteMeldinger = mutableListOf<ProducerRecord<String, String>>()

        fun resetMock(kafkaTemplate: KafkaTemplate<String, String>) {
            io.mockk.clearMocks(kafkaTemplate)
            sendteMeldinger.clear()
            every { kafkaTemplate.send(any<ProducerRecord<String, String>>()) } answers {
                val record = firstArg<ProducerRecord<String, String>>()
                sendteMeldinger.add(record)
                CompletableFuture.completedFuture(mockk())
            }
        }

        fun sendteMeldinger() = sendteMeldinger.toList()
    }
}
