package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain

import java.util.UUID

@JvmInline
value class FlyttingId(
    val id: UUID,
) {
    override fun toString(): String = id.toString()

    companion object {
        fun random() = FlyttingId(UUID.randomUUID())

        fun fromString(id: String) = FlyttingId(UUID.fromString(id))
    }
}
