package no.nav.tilleggsstonader.sak.googlemaps

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView

@RestController
@RequestMapping(path = ["/api/kart"])
@ProtectedWithClaims(issuer = "azuread")
class GooglemapsController(
    private val googleAutocompleteClient: GoogleAutocompleteClient,
    private val staticMapClient: GoogleStaticMapClient,
    private val googlemapsService: GooglemapsService,
    private val googleEmbeddedMapClient: GoogleEmbeddedMapClient,
) {
    @PostMapping("/kjoreavstand")
    fun hentKjoreavstand(
        @RequestBody finnReiseAvstandDto: FinnReiseavstandDto,
    ): ReisedataDto? =
        googlemapsService
            .hentKjøreruter(
                fraAdresse = Address(finnReiseAvstandDto.fraAdresse),
                tilAdresse = Address(finnReiseAvstandDto.tilAdresse),
            )

    @PostMapping("/kollektiv-detaljer")
    fun hentKollektivDetalher(
        @RequestBody finnReiseAvstandDto: FinnReiseavstandDto,
    ) = googlemapsService
        .hentKollektivRute(
            fraAdresse = Address(finnReiseAvstandDto.fraAdresse),
            tilAdresse = Address(finnReiseAvstandDto.tilAdresse),
        )

    @PostMapping("/statisk-kart")
    fun hentStatiskKart(
        @RequestBody statiskKartRequest: StatiskKartRequest,
    ): ByteArray? = staticMapClient.hentStaticMap(statiskKartRequest)


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
