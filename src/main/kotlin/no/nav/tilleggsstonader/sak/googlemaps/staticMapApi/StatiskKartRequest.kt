package no.nav.tilleggsstonader.sak.googlemaps.staticMapApi

import no.nav.tilleggsstonader.sak.googlemaps.dto.LokasjonDto

data class StatiskKartRequest(
    val polyline: String,
    val startLokasjon: LokasjonDto,
    val sluttLokasjon: LokasjonDto,
)
