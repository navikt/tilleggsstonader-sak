package no.nav.tilleggsstonader.sak.felles.domain

import java.util.UUID

@JvmInline
value class BehandlingId(
    val id: UUID,
) {

    /**
     * Vurder å finne de som bruker tostring og erstatt med noe annet?
     */
    override fun toString(): String {
        return id.toString()
    }

    companion object {
        fun random() = BehandlingId(UUID.randomUUID())
        fun fromString(id: String) = BehandlingId(UUID.fromString(id))
    }
}
