package no.nav.tilleggsstonader.sak.nvdbApi

data class NvdbBomstasjonResponse(
    val objekter: List<NvdbObjekt>,
    val metadata: NvdbMetadata,
)

data class NvdbObjekt(
    val id: Long,
    val egenskaper: List<NvdbEgenskaper>?,
    val lokasjon: NvdbLokasjon?,
)

data class NvdbLokasjon(
    val geometri: NvdbGeometri?,
)

data class NvdbEgenskaper(
    val id: Int,
    val navn: String,
    val verdi: String,
    val egenskapstype: String,
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
    val navn: String?,
    val takstLitenBilRush: Double?,
    val lat: Double,
    val lng: Double,
)

private val WKT_REGEX = Regex("""POINT (?:Z )?\((-?\d+\.\d+) (-?\d+\.\d+)""")

private fun List<NvdbEgenskaper>?.finnEgenskapVerdi(egenskapNavn: String): String? = this?.find { it.navn == egenskapNavn }?.verdi

fun NvdbObjekt.tilDomene(): NvdbBomstasjon? {
    val wkt = lokasjon?.geometri?.wkt ?: return null
    val match = WKT_REGEX.find(wkt) ?: return null
    // Kan brukes på et senere tidspunkt til å vise hvilke bomstasjoner man passerer og prisene
    val navn = egenskaper?.finnEgenskapVerdi("Navn bomstasjon") ?: return null
    val takstLitenBilRush = egenskaper.finnEgenskapVerdi("Rushtidstakst liten bil")?.toDoubleOrNull()
    val (lat, lng) = match.destructured
    return NvdbBomstasjon(
        id = id,
        navn = navn,
        takstLitenBilRush = takstLitenBilRush,
        lat = lat.toDouble(),
        lng = lng.toDouble(),
    )
}
/* Research på beregning av pris:
    Praksis er 20% og alltid rushpriser
    Gi alltid 20% med autosync - det er også standarden til bompengekalkulatoren

    Diesel = takstLitenBilRush
    Bensin = Diesel × 0,9048 (Litt usikker på denne - gir ikke alltid riktig)
    Elbil = diesel / 2
 */
