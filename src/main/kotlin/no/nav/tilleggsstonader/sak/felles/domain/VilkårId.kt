package no.nav.tilleggsstonader.sak.felles.domain

import java.util.UUID

@JvmInline
value class VilkårId(
    val id: UUID,
) {
    /**
     * Vurder å finne de som bruker tostring og erstatt med noe annet?
     */
    override fun toString(): String = id.toString()

    companion object {
        fun random() = VilkårId(UUID.randomUUID())

        fun fromString(id: String) = VilkårId(UUID.fromString(id))
    }
}
