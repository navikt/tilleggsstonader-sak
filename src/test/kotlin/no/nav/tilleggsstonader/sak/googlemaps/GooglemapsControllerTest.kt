package no.nav.tilleggsstonader.sak.googlemaps

import io.mockk.every
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.GeocodedWaypoint
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.GeocodingResults
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.LatLng
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.Leg
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.Location
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.Polyline
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.Reisetype
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.Route
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.RuteResponse
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.Step
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class GooglemapsControllerTest : IntegrationTest() {
    @Autowired
    lateinit var kjøreavstandLoggRepository: KjøreavstandLoggRepository

    @Test
    fun `skal lagre søk etter kjøreavstand i database`() {
        val fraAdresse = "Myravegen 2, 6360 Åfarnes"
        val tilAdresse = "Tjugenfossen, 6789 Loen"

        every { mockClientService.googleRoutesClient.hentRuter(any()) } returns dummyRute
        every { mockClientService.googlePlaceDetailsClient.finnStedDetaljer("test-origin-place-id") } returns
            no.nav.tilleggsstonader.sak.googlemaps.placeDetailsApi.PlaceDetailsResponse(
                id = "test-origin-place-id",
                formattedAddress = fraAdresse,
                displayName = null,
            )
        every { mockClientService.googlePlaceDetailsClient.finnStedDetaljer("test-destination-place-id") } returns
            no.nav.tilleggsstonader.sak.googlemaps.placeDetailsApi.PlaceDetailsResponse(
                id = "test-destination-place-id",
                formattedAddress = tilAdresse,
                displayName = null,
            )

        val resultatFør = kjøreavstandLoggRepository.findAll().toList()

        medBrukercontext<Unit>(bruker = "saksbehandler123") {
            kall.kart.hentKjøreavstand(fraAdresse, tilAdresse)
        }

        val resultatEtter = kjøreavstandLoggRepository.findAll().toList()

        assertThat(resultatEtter).hasSize(resultatFør.size + 1)

        val nyRadLagret = resultatEtter.last()
        with(nyRadLagret) {
            assertThat(sporring.json).contains(fraAdresse)
            assertThat(sporring.json).contains(tilAdresse)
            assertThat(resultat?.json).isNotNull()
            assertThat(resultat?.json).contains("10000")
            assertThat(saksbehandler).isEqualTo("saksbehandler123")
            assertThat(tidspunkt).isNotNull()
        }
    }
}

val dummySteps =
    listOf(
        Step(
            travelMode = Reisetype.DRIVE,
            startLocation =
                Location(
                    latLng =
                        LatLng(
                            latitude = 62.659176200000005,
                            longitude = 7.502830500000001,
                        ),
                ),
            endLocation =
                Location(
                    latLng =
                        LatLng(
                            latitude = 61.837784799999994,
                            longitude = 6.706426899999999,
                        ),
                ),
            navigationInstruction = null,
            transitDetails = null,
            distanceMeters = 10000,
            staticDuration = "3400s",
        ),
    )

var dummyRute =
    RuteResponse(
        routes =
            listOf(
                Route(
                    routeLabels = emptyList(),
                    polyline = Polyline(""),
                    distanceMeters = 10000,
                    staticDuration = "200s",
                    legs = listOf(Leg(steps = dummySteps)),
                ),
            ),
        geocodingResults =
            GeocodingResults(
                origin = GeocodedWaypoint(placeId = "test-origin-place-id"),
                destination = GeocodedWaypoint(placeId = "test-destination-place-id"),
            ),
    )
