package no.nav.tilleggsstonader.sak.googlemaps

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.googlemaps.dto.FinnReiseavstandDto
import no.nav.tilleggsstonader.sak.googlemaps.dto.ReisedataDto
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.Address
import no.nav.tilleggsstonader.sak.googlemaps.staticMapApi.GoogleStaticMapClient
import no.nav.tilleggsstonader.sak.googlemaps.staticMapApi.StatiskKartRequest
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/kart"])
@ProtectedWithClaims(issuer = "azuread")
class GooglemapsController(
    private val staticMapClient: GoogleStaticMapClient,
    private val googlemapsService: GooglemapsService,
    private val kjøreavstandLoggRepository: KjøreavstandLoggRepository,
) {
    @PostMapping("/kjoreavstand")
    fun hentKjoreavstand(
        @RequestBody finnReiseAvstandDto: FinnReiseavstandDto,
    ): ReisedataDto? {
        val kjørerute =
            googlemapsService
                .hentKjøreruter(
                    fraAdresse = Address(finnReiseAvstandDto.fraAdresse),
                    tilAdresse = Address(finnReiseAvstandDto.tilAdresse),
                )

        kjøreavstandLoggRepository.insert(
            KjøreavstandLogg(
                sporring = JsonWrapper(objectMapper.writeValueAsString(finnReiseAvstandDto)),
                resultat = kjørerute?.let { JsonWrapper(objectMapper.writeValueAsString(it)) },
            ),
        )

        return kjørerute
    }

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
}
