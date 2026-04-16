package no.nav.tilleggsstonader.sak.privatbil

import java.util.UUID

@JvmInline
value class KjørelisteId(
    val id: UUID,
) {
    override fun toString(): String = id.toString()

    companion object {
        fun random() = KjørelisteId(UUID.randomUUID())

        fun fromString(id: String) = KjørelisteId(UUID.fromString(id))
    }
}
