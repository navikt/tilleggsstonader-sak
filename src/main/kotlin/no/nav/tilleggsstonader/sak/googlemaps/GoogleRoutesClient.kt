package no.nav.tilleggsstonader.sak.googlemaps

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.client.bodyWithType
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class GoogleRoutesClient(
    @Value("https://routes.googleapis.com/directions/v2:computeRoutes")
    private val baseUrl: URI,
    @Value($$"${GOOGLE_MAPS_API_KEY}") private val apiKey: String,
    builder: RestClient.Builder,
) {
    private val restClient = builder.baseUrl(baseUrl.toString()).build()
    private val uri = UriComponentsBuilder.fromUri(baseUrl).encode().toUriString()

    fun hentRuter(request: RuteRequest): RuteDto =
        restClient
            .post()
            .uri(uri)
            .headers { headers ->
                headers.apply {
                    add("X-Goog-Api-Key", apiKey)
                    add(
                        "X-Goog-FieldMask",
                        "*",
                    )
                    add("Content-Type", "application/json")
                }
            }.bodyWithType(request)
            .retrieve()
            .body<RuteDto>()!!
}

data class RuteRequest(
    val origin: Address,
    val destination: Address,
    val travelMode: String,
    val departureTime: String,
    val transitPreferences: TransitPreferences,
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

data class RuteDto(
    val routes: List<Route>?,
)

data class Route(
    val distanceMeters: Int?,
    val duration: String?,
    val legs: List<Leg>?,
)

data class Leg(
    val steps: List<Step>?,
)

data class Step(
    val travelMode: String?,
    val startLocation: Location?,
    val endLocation: Location?,
    val transitDetails: TransitDetails?,
    val distanceMeters: Int?,
    val staticDuration: String?,
)

data class TransitDetails(
    val stopDetails: StopDetails?,
    val transitLine: TransitLine?,
    val stopCount: Int?,
)

data class StopDetails(
    val departureStop: Stop?,
    val arrivalStop: Stop?,
)

data class Stop(
    val name: String?,
    val location: Location?,
)

data class Location(
    val latLng: LatLng?,
)

data class LatLng(
    val latitude: Double?,
    val longitude: Double?,
)

data class TransitLine(
    val shortName: String?,
    val name: String?,
    val vehicle: Vehicle?,
)

data class Vehicle(
    val type: String?,
)
