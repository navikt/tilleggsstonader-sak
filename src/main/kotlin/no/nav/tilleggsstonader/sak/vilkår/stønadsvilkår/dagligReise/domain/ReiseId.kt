package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain

import java.util.UUID

@JvmInline
value class ReiseId(
    val id: UUID,
) {
    override fun toString(): String = id.toString()

    companion object {
        fun random() = ReiseId(UUID.randomUUID())

        fun fromString(id: String) = ReiseId(UUID.fromString(id))
    }
}
