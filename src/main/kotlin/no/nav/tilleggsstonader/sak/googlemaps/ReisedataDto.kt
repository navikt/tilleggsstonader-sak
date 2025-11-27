import no.nav.tilleggsstonader.sak.googlemaps.Leg
import no.nav.tilleggsstonader.sak.googlemaps.LinjeType
import no.nav.tilleggsstonader.sak.googlemaps.Location
import no.nav.tilleggsstonader.sak.googlemaps.Polyline
import no.nav.tilleggsstonader.sak.googlemaps.Reisetype
import no.nav.tilleggsstonader.sak.googlemaps.Route
import no.nav.tilleggsstonader.sak.googlemaps.Step
import no.nav.tilleggsstonader.sak.googlemaps.TransitAgency
import no.nav.tilleggsstonader.sak.googlemaps.TransitDetails

data class RuteDto(
    val polyline: Polyline,
    val avstandMeter: Int,
    val varighetSekunder: Double,
    val strekninger: List<StrekningDto>,
    val startLokasjon: Lokasjon,
    val sluttLokasjon: Lokasjon,
)

data class StrekningDto(
    val varighetSekunder: Double,
    val reisetype: Reisetype,
    val kollektivDetaljer: KollektivDetaljerDto?,
)

data class KollektivDetaljerDto(
    val startHoldeplass: String,
    val sluttHoldeplass: String,
    val linjeNavn: String,
    val linjeType: LinjeType,
    val operatør: List<Operatør>,
)

data class Operatør(
    val navn: String,
    val url: String,
)

data class Lokasjon(
    val lat: Double,
    val lng: Double,
)

fun Route.tilDto(): RuteDto =
    RuteDto(
        polyline = polyline,
        avstandMeter = distanceMeters,
        varighetSekunder = staticDuration.tilDouble(),
        strekninger = legs.tilDto(),
        startLokasjon =
            legs
                .first()
                .steps
                .first()
                .startLocation
                .tilLokasjon(),
        sluttLokasjon =
            legs
                .last()
                .steps
                .last()
                .endLocation
                .tilLokasjon(),
    )

private fun List<Leg>.tilDto(): List<StrekningDto> {
    val steps = this.flatMap { leg -> leg.steps }.mergeSammenhengende()
    return steps.map {
        StrekningDto(
            varighetSekunder = it.staticDuration.tilDouble(),
            reisetype = it.travelMode,
            kollektivDetaljer = it.transitDetails?.tilDto(),
        )
    }
}

private fun TransitDetails.tilDto(): KollektivDetaljerDto =
    KollektivDetaljerDto(
        startHoldeplass = stopDetails.departureStop.name,
        sluttHoldeplass = stopDetails.arrivalStop.name,
        linjeNavn = transitLine.name,
        linjeType = transitLine.vehicle.type,
        operatør = transitLine.agencies.map { it.tilDto() },
    )

private fun TransitAgency.tilDto(): Operatør =
    Operatør(
        navn = this.name,
        url = this.uri,
    )

private fun List<Step>.mergeSammenhengende(): List<Step> =
    this.fold(mutableListOf()) { acc, entry ->
        val last = acc.lastOrNull()
        val skalMerges =
            last != null &&
                last.travelMode == entry.travelMode &&
                last.transitDetails == entry.transitDetails
        if (skalMerges) {
            acc.removeLast()
            acc.add(
                last.copy(
                    endLocation = entry.endLocation,
                    distanceMeters = last.distanceMeters + entry.distanceMeters,
                    staticDuration = summerSekunderStrings(last.staticDuration, entry.staticDuration),
                ),
            )
        } else {
            acc.add(entry)
        }
        acc
    }

private fun Location.tilLokasjon(): Lokasjon =
    Lokasjon(
        lat = this.latLng.latitude,
        lng = this.latLng.longitude,
    )

private fun String.tilDouble(): Double = this.removeSuffix("s").toDouble()

private fun summerSekunderStrings(
    string1: String,
    string2: String,
): String = (string1.tilDouble() + string2.tilDouble()).toString() + "s"
