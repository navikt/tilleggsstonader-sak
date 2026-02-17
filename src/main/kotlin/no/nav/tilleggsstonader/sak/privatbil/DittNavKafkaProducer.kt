package no.nav.tilleggsstonader.sak.privatbil

import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service


@Service
class DittNavKafkaProducer(
    val kafkaTemplate: KafkaTemplate<String, String>,
) {
    @Value("\${topics.dittnav}")
    private lateinit var topic: String

    @Value("\${NAIS_CLUSTER_NAME}")
    private lateinit var cluster: String

    @Value("\${NAIS_NAMESPACE}")
    private lateinit var namespace: String

    @Value("\${NAIS_APP_NAME}")
    private lateinit var appName: String

    fun sendToKafka(
        // fnr: String,
        melding: String,
        eventId: String,
    ): String {
        val kafkaBeskjedJson =
            VarselActionBuilder.opprett {
                type = Varseltype.Beskjed
                varselId = eventId
                sensitivitet = Sensitivitet.Substantial
                // TODO hva må jeg sende med
                // ident = fnr
                tekster +=
                    Tekst(
                        spraakkode = "nb",
                        tekst = melding,
                        default = true,
                    )
                produsent = Produsent(
                    cluster = cluster,
                    namespace = namespace,
                    appnavn = appName,
                )
            }

        runCatching {
            val producerRecord = ProducerRecord(topic, eventId, kafkaBeskjedJson)
            kafkaTemplate.send(producerRecord).get()
        }.onFailure {
            val errorMessage = "Could not send kjoreliste to DittNav Kafka. Check secure logs for more information."
            secureLogger.error("Could not send kjoreliste to DittNav Kafka melding=$kafkaBeskjedJson", it)
            throw RuntimeException(errorMessage)
        }
        return kafkaBeskjedJson
    }
}
