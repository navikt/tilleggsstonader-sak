package no.nav.tilleggsstonader.sak.googlemaps

data class RuteRequest(
    val origin: Address,
    val destination: Address,
    val travelMode: String,
    val departureTime: String?,
    val transitPreferences: TransitPreferences?,
    val computeAlternativeRoutes: Boolean?,
)

data class Address(
    val address: String,
)

data class TransitPreferences(
    val allowedTravelModes: List<String>?,
)

enum class TransitOption(
    val value: String,
) {
    BUS("bus"),
    SUBWAY("subway"),
    TRAIN("train"),
    LIGHT_RAIL("light_rail"),
    RAIL("rail"),
}
