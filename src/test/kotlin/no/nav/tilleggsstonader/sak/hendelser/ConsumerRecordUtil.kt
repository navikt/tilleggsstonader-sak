package no.nav.tilleggsstonader.sak.hendelser

import org.apache.kafka.clients.consumer.ConsumerRecord

object ConsumerRecordUtil {
    fun <KEY, VALUE> lagConsumerRecord(
        key: KEY,
        value: VALUE,
        offset: Long = 1,
    ): ConsumerRecord<KEY, VALUE> =
        ConsumerRecord(
            "topic",
            1,
            offset,
            key,
            value,
        )
}
