package no.nav.tilleggsstonader.sak.integrasjonstest.extensions

import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import org.apache.kafka.clients.producer.ProducerRecord
import tools.jackson.module.kotlin.readValue

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

inline fun <reified T> ProducerRecord<String, String>.verdiEllerFeil(): T = jsonMapper.readValue<T>(this.value())
