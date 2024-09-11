package no.nav.tilleggsstonader.sak.felles.domain

import java.util.UUID

@JvmInline
value class FagsakId(
    val id: UUID,
) {

    override fun toString(): String {
        return id.toString()
    }

    companion object {
        fun randomUUID() = FagsakId(UUID.randomUUID())
        fun fromString(id: String) = FagsakId(UUID.fromString(id))
    }
}
