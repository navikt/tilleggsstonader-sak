package no.nav.tilleggsstonader.sak.googlemaps.nvdbApi

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

fun finnBomstasjonPåRute(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
): Double {
    val jordRadiusM = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return jordRadiusM * 2 * asin(sqrt(a))
}
