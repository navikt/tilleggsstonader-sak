package no.nav.tilleggsstonader.sak.googlemaps

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tilDto
import java.time.OffsetDateTime
import java.time.ZoneOffset

@RestController
@RequestMapping(path = ["/api/kart"])
@ProtectedWithClaims(issuer = "azuread")
class GooglemapsController(
    private val googleRoutesClient: GoogleRoutesClient,
    private val staticMapClient: GoogleStaticMapClient,
) {
    @PostMapping("/kjoreavstand")
    fun hentKjoreavstand(
        @RequestBody finnReiseAvstandDto: FinnReiseAvstandDto,
    ) = googleRoutesClient
        .hentRuter(
            RuteRequest(
                origin = Address(finnReiseAvstandDto.fraAdresse.tilSøkeString()),
                destination = Address(finnReiseAvstandDto.tilAdresse.tilSøkeString()),
                travelMode = "DRIVE",
                departureTime = null,
                transitPreferences = null,
                polylineQuality = "OVERVIEW",
            ),
        )?.finnDefaultRute()
        ?.tilDto()

    @PostMapping("/kollektiv-detaljer")
    fun hentKollektivDetalher(
        @RequestBody finnReiseAvstandDto: FinnReiseAvstandDto,
    ) = googleRoutesClient
        .hentRuter(
            RuteRequest(
                origin = Address(finnReiseAvstandDto.fraAdresse.tilSøkeString()),
                destination = Address(finnReiseAvstandDto.tilAdresse.tilSøkeString()),
                travelMode = "TRANSIT",
                departureTime = OffsetDateTime.now(ZoneOffset.UTC).toString(),
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
            ),
        )?.finnDefaultRute()
        ?.tilDto()

    @PostMapping("/statisk-kart")
    fun hentStatiskKart(
        @RequestBody statiskKartRequest: StatiskKartRequest,
    ): ByteArray? = staticMapClient.hentStaticMap(statiskKartRequest)
}
