package no.nav.tilleggsstonader.sak.googlemaps

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.time.ZoneOffset

@RestController
@RequestMapping(path = ["/api/kjoreavstand"])
@ProtectedWithClaims(issuer = "azuread")
class GooglemapsController(
    private val googleRoutesClient: GoogleRoutesClient,
) {
    @PostMapping()
    fun hentKjoreavstand(
        @RequestBody finnReiseAvstandDto: FinnReiseAvstandDto,
    ) = googleRoutesClient.hentRuter(
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
        ),
    )
}

@RestController
@RequestMapping(path = ["/api/kjoreavstand-v2"])
@ProtectedWithClaims(issuer = "azuread")
class GooglemapsControllerV2(
    private val googleRoutesClient: GoogleRoutesClient,
) {
    @PostMapping()
    fun hentKjoreavstand(
        @RequestBody finnReiseAvstandDto: FinnReiseAvstandDto,
    ) = googleRoutesClient.hentRuterV2(
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
        ),
    )
}
