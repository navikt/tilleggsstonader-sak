package no.nav.tilleggsstonader.sak.googlemaps

data class StatiskKartRequest(
    val polyline: String,
    val startLokasjon: LokasjonDto,
    val sluttLokasjon: LokasjonDto,
)
