package no.nav.tilleggsstonader.sak.googlemaps.placeDetailsApi

data class PlaceDetailsResponse(
    val id: String,
    val formattedAddress: String,
    val displayName: DisplayName?,
)

data class DisplayName(
    val text: String,
    val languageCode: String?,
)
