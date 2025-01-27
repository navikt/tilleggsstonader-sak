package no.nav.tilleggsstonader.sak.felles.domain

import java.util.UUID

@JvmInline
value class FagsakPersonId(
    val id: UUID,
) {
    override fun toString(): String = id.toString()

    companion object {
        fun random() = FagsakPersonId(UUID.randomUUID())

        fun fromString(id: String) = FagsakPersonId(UUID.fromString(id))
    }
}
