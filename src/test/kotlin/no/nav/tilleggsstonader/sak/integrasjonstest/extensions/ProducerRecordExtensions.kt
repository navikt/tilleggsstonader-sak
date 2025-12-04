package no.nav.tilleggsstonader.sak.integrasjonstest.extensions

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.jsonMapper
import org.apache.kafka.clients.producer.ProducerRecord
import tools.jackson.module.kotlin.readValue

fun List<ProducerRecord<String, String>>.finnPåTopic(topic: String): List<ProducerRecord<String, String>> = filter { it.topic() == topic }

fun List<ProducerRecord<String, String>>.tellAntallPåTopic(topic: String): Int = finnPåTopic(topic).size

fun List<ProducerRecord<String, String>>.forventAntallMeldingerPåTopic(
    topic: String,
    forventetAntall: Int,
) {
    val actual = tellAntallPåTopic(topic)
    require(actual == forventetAntall) {
        "Forventet $forventetAntall meldinger på topic '$topic', men fant $actual"
    }
}

inline fun <reified T> ProducerRecord<String, String>.verdiEllerFeil(): T = jsonMapper.readValue<T>(this.value())
