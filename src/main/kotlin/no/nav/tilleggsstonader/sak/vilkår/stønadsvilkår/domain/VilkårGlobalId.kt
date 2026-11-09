package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain

import java.util.UUID

@JvmInline
value class VilkårGlobalId(
    val id: UUID,
) {
    override fun toString(): String = id.toString()

    companion object {
        fun random() = VilkårGlobalId(UUID.randomUUID())

        fun fromString(id: String) = VilkårGlobalId(UUID.fromString(id))
    }
}
