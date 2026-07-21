package no.nav.tilleggsstonader.sak.felles.domain

import java.util.UUID

@JvmInline
value class VedtaksperiodeId(
    val id: UUID,
) {
    override fun toString(): String = id.toString()

    companion object {
        fun random() = VedtaksperiodeId(UUID.randomUUID())

        fun fromString(id: String) = VedtaksperiodeId(UUID.fromString(id))
    }
}
