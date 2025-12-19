package no.nav.tilleggsstonader.sak.googlemaps

data class Rute(
    val erDefualtRute: Boolean,
    val polyline: Polyline,
    val avstandMeter: Int,
    val varighetSekunder: Double,
    val strekninger: List<Strekning>,
    val startLokasjon: Lokasjon,
    val sluttLokasjon: Lokasjon,
    val startLokasjonId: String?,
    val sluttLokasjonId: String?,
)

data class Strekning(
    val varighetSekunder: Double,
    val reisetype: Reisetype,
    val kollektivDetaljer: KollektivDetaljer?,
    val erFerje: Boolean,
    val avstandMeter: Int,
)

data class KollektivDetaljer(
    val startHoldeplass: String,
    val sluttHoldeplass: String,
    val linjeNavn: String?,
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

data class StartOgSluttAdresse(
    val startAdresse: String,
    val sluttAdresse: String,
)

fun RuteResponse.tilDomene(): List<Rute>? =
    this.routes?.map {
        it.tilDomene(
            startLokasjonId = this.geocodingResults.origin.placeId,
            sluttLokasjonId = this.geocodingResults.destination.placeId,
        )
    }

fun Route.tilDomene(
    startLokasjonId: String?,
    sluttLokasjonId: String?,
): Rute =
    Rute(
        erDefualtRute = routeLabels.contains(RouteLabel.DEFAULT_ROUTE),
        polyline = polyline,
        avstandMeter = distanceMeters ?: 0,
        varighetSekunder = staticDuration.tilDouble(),
        strekninger = legs.tilDomene(),
        startLokasjon = finnStartLokasjon(),
        sluttLokasjon = finnSluttLokasjon(),
        startLokasjonId = startLokasjonId,
        sluttLokasjonId = sluttLokasjonId,
    )

fun List<Leg>.tilDomene(): List<Strekning> {
    val steps = this.flatMap { leg -> leg.steps }.mergeSammenhengende()
    return steps.map {
        Strekning(
            varighetSekunder = it.staticDuration.tilDouble(),
            reisetype = it.travelMode,
            kollektivDetaljer = it.transitDetails?.tilDomene(),
            erFerje = it.navigationInstruction?.maneuver == Maneuver.FERRY,
            avstandMeter = it.distanceMeters ?: 0,
        )
    }
}

private fun TransitDetails.tilDomene(): KollektivDetaljer =
    KollektivDetaljer(
        startHoldeplass = stopDetails.departureStop.name,
        sluttHoldeplass = stopDetails.arrivalStop.name,
        linjeNavn = transitLine.name,
        linjeType = transitLine.vehicle.type,
        operatør = transitLine.agencies.map { it.tilDomene() },
    )

private fun TransitAgency.tilDomene(): Operatør =
    Operatør(
        navn = this.name,
        url = this.uri,
    )

fun Location.tilDomene() =
    Lokasjon(
        lat = latLng.latitude,
        lng = latLng.longitude,
    )

fun Route.finnStartLokasjon() =
    legs
        .first()
        .steps
        .first()
        .startLocation
        .tilDomene()

fun Route.finnSluttLokasjon() =
    legs
        .last()
        .steps
        .last()
        .endLocation
        .tilDomene()

private fun List<Step>.mergeSammenhengende(): List<Step> =
    this.fold(mutableListOf()) { acc, entry ->
        val last = acc.lastOrNull()
        val skalMerges =
            last != null &&
                last.travelMode == entry.travelMode &&
                last.transitDetails == entry.transitDetails &&
                last.navigationInstruction?.maneuver != Maneuver.FERRY &&
                entry.navigationInstruction?.maneuver != Maneuver.FERRY
        if (skalMerges) {
            acc.removeLast()
            acc.add(
                last.copy(
                    endLocation = entry.endLocation,
                    distanceMeters = listOfNotNull(last.distanceMeters, entry.distanceMeters).sum(),
                    staticDuration = summerSekunderStrings(last.staticDuration, entry.staticDuration),
                ),
            )
        } else {
            acc.add(entry)
        }
        acc
    }

fun String.tilDouble(): Double = this.removeSuffix("s").toDouble()

private fun summerSekunderStrings(
    string1: String,
    string2: String,
): String = (string1.tilDouble() + string2.tilDouble()).toString() + "s"
