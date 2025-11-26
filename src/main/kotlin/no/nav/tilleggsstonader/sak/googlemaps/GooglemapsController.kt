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
    private val googleAutocompleteClient: GoogleAutocompleteClient,
) {
    @PostMapping("/kjoreavstand")
    fun hentKjoreavstand(
        @RequestBody finnReiseAvstandDto: FinnReiseAvstandDto,
    ) = googleRoutesClient
        .hentRuter(
            RuteRequest(
                origin = Address(finnReiseAvstandDto.fraAdresse),
                destination = Address(finnReiseAvstandDto.tilAdresse),
                travelMode = "DRIVE",
                departureTime = null,
                transitPreferences = null,
            ),
        )?.finnDefaultRute()
        ?.tilDto()

    @PostMapping("/kollektiv-detaljer")
    fun hentKollektivDetalher(
        @RequestBody finnReiseAvstandDto: FinnReiseAvstandDto,
    ) = googleRoutesClient
        .hentRuter(
            RuteRequest(
                origin = Address(finnReiseAvstandDto.fraAdresse),
                destination = Address(finnReiseAvstandDto.tilAdresse),
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
        )?.finnDefaultRute()
        ?.tilDto()

    @PostMapping("/autocomplete")
    fun hentForslag(
        @RequestBody hentForslagDto: HentForslagDto,
    ) = googleAutocompleteClient
        .hentForslag(
            AutocompleteRequest(
                input = hentForslagDto.input,
                includedRegionCodes = listOf("no"),
                languageCode = "no",
                regionCode = "no",
            ),
        )?.tilDto()
}
