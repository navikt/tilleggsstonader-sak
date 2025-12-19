package no.nav.tilleggsstonader.sak.googlemaps

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId

@Service
class GooglemapsService(
    val googleRoutesClient: GoogleRoutesClient,
    val googlePlaceDetailsClient: GooglePlaceDetailsClient,
) {
    fun hentKjøreruter(
        fraAdresse: Address,
        tilAdresse: Address,
    ): ReisedataDto? {
        val ruteForslag =
            googleRoutesClient
                .hentRuter(
                    RuteRequest(
                        origin = fraAdresse,
                        destination = tilAdresse,
                        travelMode = "DRIVE",
                        departureTime = null,
                        transitPreferences = null,
                        polylineQuality = "OVERVIEW",
                        computeAlternativeRoutes = true,
                    ),
                )?.tilDomene()

        val kortesteRute = ruteForslag?.finnKortesteRute()
        feilHvis(kortesteRute == null) { "Kunne ikke finne reiserute fra ${fraAdresse.address} til ${tilAdresse.address}" }

        val startOgSluttAdresse = finnStartOgSluttAdresse(kortesteRute)
        val avstandUtenFerje = kortesteRute.avstandMeter - kortesteRute.finnFerjeavstand()

        return ReisedataDto(
            rute = kortesteRute,
            startOgSluttAdresse = startOgSluttAdresse,
            avstandUtenFerje = avstandUtenFerje,
        )
    }

    fun hentKollektivRute(
        fraAdresse: Address,
        tilAdresse: Address,
    ): ReisedataDto? {
        val ruteForslag =
            googleRoutesClient
                .hentRuter(
                    RuteRequest(
                        origin = fraAdresse,
                        destination = tilAdresse,
                        travelMode = "TRANSIT",
                        departureTime =
                            LocalDate
                                .now()
                                .atTime(8, 0)
                                .atZone(ZoneId.of("Europe/Oslo"))
                                .toInstant()
                                .toString(),
                        transitPreferences =
                            TransitPreferences(
                                allowedTravelModes =
                                    listOf(
                                        TransitOption.TRAIN.value,
                                        TransitOption.SUBWAY.value,
                                        TransitOption.BUS.value,
                                        TransitOption.LIGHT_RAIL.value,
                                        TransitOption.RAIL.value,
                                    ),
                            ),
                        polylineQuality = "OVERVIEW",
                        computeAlternativeRoutes = false,
                    ),
                )?.tilDomene()

        return ruteForslag?.finnDefaultRute()?.let { defaultRute ->
            val startOgSluttAdresse = finnStartOgSluttAdresse(defaultRute)
            val avstandUtenFerje = defaultRute.avstandMeter - defaultRute.finnFerjeavstand()

            ReisedataDto(
                rute = defaultRute,
                startOgSluttAdresse = startOgSluttAdresse,
                avstandUtenFerje = avstandUtenFerje,
            )
        }
    }

    private fun finnStartOgSluttAdresse(rute: Rute): StartOgSluttAdresse {
        val startAdresse = rute.startLokasjonId?.let { googlePlaceDetailsClient.finnStedDetaljer(it)?.formattedAddress }
        val sluttAdresse = rute.sluttLokasjonId?.let { googlePlaceDetailsClient.finnStedDetaljer(it)?.formattedAddress }

        feilHvis(startAdresse == null || sluttAdresse == null) { "Kunne ikke finne start eller slutt adresse for reise" }
        return StartOgSluttAdresse(
            startAdresse = startAdresse,
            sluttAdresse = sluttAdresse,
        )
    }

    private fun Rute.finnFerjeavstand(): Int = strekninger.filter { it.erFerje }.sumOf { it.avstandMeter }

    fun List<Rute>.finnKortesteRute() = minBy { it.avstandMeter }

    fun List<Rute>.finnDefaultRute() = find { it.erDefualtRute }
}
