package no.nav.tilleggsstonader.sak.googlemaps.nvdbApi

data class NvdbBomstasjonResponse(
    val objekter: List<NvdbObjekt>,
    val metadata: NvdbMetadata,
)

data class NvdbObjekt(
    val id: Long,
    val lokasjon: NvdbLokasjon?,
)

data class NvdbLokasjon(
    val geometri: NvdbGeometri?,
)

data class NvdbGeometri(
    val wkt: String,
    val srid: Int,
)

data class NvdbMetadata(
    val neste: NvdbNeste?,
)

data class NvdbNeste(
    val start: String,
)

data class NvdbBomstasjon(
    val id: Long,
    val lat: Double,
    val lng: Double,
)

private val WKT_REGEX = Regex("""POINT (?:Z )?\((-?\d+\.\d+) (-?\d+\.\d+)""")

fun NvdbObjekt.tilDomene(): NvdbBomstasjon? {
    val wkt = lokasjon?.geometri?.wkt ?: return null
    val match = WKT_REGEX.find(wkt) ?: return null
    val (lat, lng) = match.destructured
    return NvdbBomstasjon(
        id = id,
        lat = lat.toDouble(),
        lng = lng.toDouble(),
    )
}
