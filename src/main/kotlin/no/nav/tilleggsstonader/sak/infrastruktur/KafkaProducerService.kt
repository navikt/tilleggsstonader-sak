package no.nav.tilleggsstonader.sak.infrastruktur

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaProducerService(private val kafkaTemplate: KafkaTemplate<String, String>) {

    fun sendMedStønadstypeIHeader(topic: String, stønadstype: Stønadstype, key: String, payload: String) {
        val record = ProducerRecord(topic, key, payload)
        record.headers().add(RecordHeader("stønadstype", stønadstype.name.toByteArray()))
        kafkaTemplate.send(record).get()
    }
}
