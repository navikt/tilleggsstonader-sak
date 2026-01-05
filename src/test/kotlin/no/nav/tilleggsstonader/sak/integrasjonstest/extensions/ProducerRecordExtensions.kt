package no.nav.tilleggsstonader.sak.integrasjonstest.extensions

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import org.apache.kafka.clients.producer.ProducerRecord

fun List<ProducerRecord<String, String>>.finnPåTopic(topic: String): List<ProducerRecord<String, String>> = filter { it.topic() == topic }

fun List<ProducerRecord<String, String>>.forventAntallMeldingerPåTopic(
    topic: String,
    forventetAntall: Int,
): List<ProducerRecord<String, String>> {
    val recordsPåTopic = finnPåTopic(topic)
    require(recordsPåTopic.size == forventetAntall) {
        "Forventet $forventetAntall meldinger på topic '$topic', men fant ${recordsPåTopic.size}"
    }
    return recordsPåTopic
}

inline fun <reified T> ProducerRecord<String, String>.verdiEllerFeil(): T = objectMapper.readValue<T>(this.value())
