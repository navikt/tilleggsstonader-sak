package no.nav.tilleggsstonader.sak.googlemaps.dto

import no.nav.tilleggsstonader.sak.googlemaps.KollektivDetaljer
import no.nav.tilleggsstonader.sak.googlemaps.Lokasjon
import no.nav.tilleggsstonader.sak.googlemaps.Operatør
import no.nav.tilleggsstonader.sak.googlemaps.Rute
import no.nav.tilleggsstonader.sak.googlemaps.StartOgSluttAdresse
import no.nav.tilleggsstonader.sak.googlemaps.Strekning
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.LinjeType
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.Polyline
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.Reisetype

data class ReisedataDto(
    val reiserute: RuteDto?,
) {
    constructor(rute: Rute, startOgSluttAdresse: StartOgSluttAdresse, avstandUtenFerje: Int) : this(
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
}

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

fun `Operatør`.tilDto() =
    OperatørDto(
        navn = navn,
        url = url,
    )
