package no.nav.tilleggsstonader.sak.nvdbApi

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(
    val lat: Double,
    val lng: Double,
)

fun String.decodePolyline(): List<GeoPoint> {
    val punkter = mutableListOf<GeoPoint>()
    var index = 0
    var lat = 0
    var lng = 0
    while (index < length) {
        var result = 0
        var shift = 0
        var b: Int
        do {
            b = this[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        lat += if (result and 1 != 0) (result ushr 1).inv() else result ushr 1

        result = 0
        shift = 0
        do {
            b = this[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        lng += if (result and 1 != 0) (result ushr 1).inv() else result ushr 1

        punkter.add(GeoPoint(lat / 1e5, lng / 1e5))
    }
    return punkter
}

fun beregnAvstandMellomPunkterMeter(
    fraLat: Double,
    fraLng: Double,
    tilLat: Double,
    tilLng: Double,
): Double {
    val jordRadiusM = 6_371_000.0
    val dLat = Math.toRadians(tilLat - fraLat)
    val dLon = Math.toRadians(tilLng - fraLng)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(fraLat)) * cos(Math.toRadians(tilLat)) * sin(dLon / 2).pow(2)
    return jordRadiusM * 2 * asin(sqrt(a))
}

/**
 * Korteste avstand (meter) fra et punkt til linjestykket p1→p2.
 * Finner nærmeste punkt på linjen ved å sammenligne punktet med punktene på linjen.
 */
fun beregnKortestAvstandFraPunktTilLinjestykke(
    punktLat: Double,
    punktLng: Double,
    p1: GeoPoint,
    p2: GeoPoint,
): Double {
    val meterPerLatGrad = 111_320.0
    val cosLat = cos(Math.toRadians((p1.lat + p2.lat) / 2.0))
    val meterPerLngGrad = meterPerLatGrad * cosLat

    val abX = (p2.lng - p1.lng) * meterPerLngGrad
    val abY = (p2.lat - p1.lat) * meterPerLatGrad
    val apX = (punktLng - p1.lng) * meterPerLngGrad
    val apY = (punktLat - p1.lat) * meterPerLatGrad

    val ab2 = abX * abX + abY * abY
    if (ab2 == 0.0) return beregnAvstandMellomPunkterMeter(p1.lat, p1.lng, punktLat, punktLng)

    val t = ((apX * abX + apY * abY) / ab2).coerceIn(0.0, 1.0)

    val narmesteSegmentpunktLat = p1.lat + t * (p2.lat - p1.lat)
    val narmesteSegmentpunktLng = p1.lng + t * (p2.lng - p1.lng)

    return beregnAvstandMellomPunkterMeter(narmesteSegmentpunktLat, narmesteSegmentpunktLng, punktLat, punktLng)
}

/**
 * Korteste avstand (meter) fra et punkt til en rute (liste med punkter).
 * Returnerer [Double.MAX_VALUE] for tom liste.
 */
fun beregnKortestAvstandFraPunktTilRute(
    punktLat: Double,
    punktLng: Double,
    rutePunkter: List<GeoPoint>,
): Double =
    rutePunkter
        .zipWithNext()
        .minOfOrNull { (p1, p2) -> beregnKortestAvstandFraPunktTilLinjestykke(punktLat, punktLng, p1, p2) }
        ?: Double.MAX_VALUE
