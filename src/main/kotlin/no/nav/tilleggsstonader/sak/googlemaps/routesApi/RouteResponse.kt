package no.nav.tilleggsstonader.sak.googlemaps.routesApi

data class RuteResponse(
    val routes: List<Route>?,
    val geocodingResults: GeocodingResults,
)

data class Route(
    val routeLabels: List<RouteLabel>,
    val polyline: Polyline,
    val distanceMeters: Int?,
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
    val navigationInstruction: NavigationInstruction?,
    val transitDetails: TransitDetails?,
    val distanceMeters: Int?,
    val staticDuration: String,
)

data class NavigationInstruction(
    val maneuver: Maneuver?,
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
    val name: String?,
    val vehicle: Vehicle,
    val agencies: List<TransitAgency>,
)

data class TransitAgency(
    val name: String,
    val uri: String,
)

data class Vehicle(
    val type: LinjeType,
)

data class Polyline(
    val encodedPolyline: String,
)

data class GeocodingResults(
    val origin: GeocodedWaypoint,
    val destination: GeocodedWaypoint,
)

data class GeocodedWaypoint(
    val placeId: String?,
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

enum class RouteLabel {
    ROUTE_LABEL_UNSPECIFIED,
    DEFAULT_ROUTE,
    DEFAULT_ROUTE_ALTERNATE,
    FUEL_EFFICIENT,
    SHORTER_DISTANCE,
}

enum class Maneuver {
    MANEUVER_UNSPECIFIED,
    TURN_SLIGHT_LEFT,
    TURN_SHARP_LEFT,
    UTURN_LEFT,
    TURN_LEFT,
    TURN_SLIGHT_RIGHT,
    TURN_SHARP_RIGHT,
    UTURN_RIGHT,
    TURN_RIGHT,
    STRAIGHT,
    RAMP_LEFT,
    RAMP_RIGHT,
    MERGE,
    FORK_LEFT,
    FORK_RIGHT,
    FERRY,
    FERRY_TRAIN,
    ROUNDABOUT_LEFT,
    ROUNDABOUT_RIGHT,
    DEPART,
    NAME_CHANGE,
}
