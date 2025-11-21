package no.nav.tilleggsstonader.sak.googlemaps

data class RuteResponse(
    val routes: List<Route>,
)

data class Route(
    val distanceMeters: Int,
    val staticDuration: String,
    val legs: List<Leg>,
)

data class Leg(
    val steps: List<Step>,
)

data class Step(
    val travelMode: Reisetype,
    val startLocation: Location,
    val endLocation: Location,
    val transitDetails: TransitDetails?,
    val distanceMeters: Int,
    val staticDuration: String,
)

data class TransitDetails(
    val stopDetails: StopDetails,
    val transitLine: TransitLine,
    val stopCount: Int?,
)

data class StopDetails(
    val departureStop: Stop,
    val arrivalStop: Stop,
)

data class Stop(
    val name: String,
    val location: Location,
)

data class Location(
    val latLng: LatLng,
)

data class LatLng(
    val latitude: Double,
    val longitude: Double,
)

data class TransitLine(
    val shortName: String?,
    val name: String,
    val vehicle: Vehicle,
)

data class Vehicle(
    val type: LinjeType,
)

enum class LinjeType {
    BUS,
    CABLE_CAR,
    COMMUTER_TRAIN,
    FERRY,
    FUNICULAR,
    GONDOLA_LIFT,
    HEAVY_RAIL,
    HIGH_SPEED_TRAIN,
    INTERCITY_BUS,
    LONG_DISTANCE_TRAIN,
    METRO_RAIL,
    MONORAIL,
    OTHER,
    RAIL,
    SHARE_TAXI,
    SUBWAY,
    TRAM,
    TROLLEYBUS,
}

enum class Reisetype {
    TRAVEL_MODE_UNSPECIFIED,
    DRIVE,
    BICYCLE,
    WALK,
    TWO_WHEELER,
    TRANSIT,
}
