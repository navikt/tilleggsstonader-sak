package no.nav.tilleggsstonader.sak.googlemaps

import com.fasterxml.jackson.annotation.JsonValue

@JvmInline
value class PlaceId(
    @get:JsonValue
    val value: String,
) {
    override fun toString(): String = value
}
