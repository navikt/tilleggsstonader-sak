package no.nav.tilleggsstonader.sak.googlemaps

data class ReisedataDto(
    val reiserute: RuteDto?,
)

data class RuteDto(
    val polyline: Polyline,
    val avstandMeter: Int,
    val avstandUtenFerje: Int,
    val varighetSekunder: Double,
    val strekninger: List<StrekningDto>,
    val startLokasjon: LokasjonDto,
    val sluttLokasjon: LokasjonDto,
    val startAdresse: String?,
    val sluttAdresse: String?,
)

data class StrekningDto(
    val varighetSekunder: Double,
    val reisetype: Reisetype,
    val kollektivDetaljer: KollektivDetaljerDto?,
)

data class KollektivDetaljerDto(
    val startHoldeplass: String,
    val sluttHoldeplass: String,
    val linjeNavn: String?,
    val linjeType: LinjeType,
    val operatør: List<OperatørDto>,
)

data class OperatørDto(
    val navn: String,
    val url: String,
)

data class LokasjonDto(
    val lat: Double,
    val lng: Double,
)

fun tilReisedataDto(
    rute: Rute,
    startOgSluttAdresse: StartOgSluttAdresse,
    avstandUtenFerje: Int,
): ReisedataDto =
    (
        ReisedataDto(
            reiserute =
                RuteDto(
                    polyline = rute.polyline,
                    avstandMeter = rute.avstandMeter,
                    avstandUtenFerje = avstandUtenFerje,
                    varighetSekunder = rute.varighetSekunder,
                    strekninger = rute.strekninger.map { it.tilDto() },
                    startLokasjon = rute.startLokasjon.tilDto(),
                    sluttLokasjon = rute.sluttLokasjon.tilDto(),
                    startAdresse = startOgSluttAdresse.startAdresse,
                    sluttAdresse = startOgSluttAdresse.sluttAdresse,
                ),
        )
    )

fun Lokasjon.tilDto() =
    LokasjonDto(
        lat = lat,
        lng = lng,
    )

fun Strekning.tilDto() =
    StrekningDto(
        varighetSekunder = varighetSekunder,
        reisetype = reisetype,
        kollektivDetaljer = kollektivDetaljer?.tilDto(),
    )

fun KollektivDetaljer.tilDto() =
    KollektivDetaljerDto(
        startHoldeplass = startHoldeplass,
        sluttHoldeplass = sluttHoldeplass,
        linjeNavn = linjeNavn,
        linjeType = linjeType,
        operatør = operatør.map { it.tilDto() },
    )

fun Operatør.tilDto() =
    OperatørDto(
        navn = navn,
        url = url,
    )
