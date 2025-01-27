package no.nav.tilleggsstonader.sak.felles.domain

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.util.UUID

@JsonDeserialize(keyUsing = BarnIdKeyDeserializer::class)
@JvmInline
value class BarnId(
    val id: UUID,
) {
    /**
     * Vurder å finne de som bruker tostring og erstatt med noe annet?
     */
    override fun toString(): String = id.toString()

    companion object {
        fun random() = BarnId(UUID.randomUUID())

        fun fromString(id: String) = BarnId(UUID.fromString(id))
    }
}
