package no.nav.tilleggsstonader.sak.googlemaps

data class StatiskKartRequest(
    val polyline: String,
    val startLokasjon: Lokasjon,
    val sluttLokasjon: Lokasjon,
)
