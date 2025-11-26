package no.nav.tilleggsstonader.sak.googlemaps

import Lokasjon

data class StatiskKartRequest(
    val polyline: String,
    val startLokasjon: Lokasjon,
    val sluttLokasjon: Lokasjon,
)
