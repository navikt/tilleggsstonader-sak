package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.googlemaps.placeDetailsApi.GooglePlaceDetailsClient
import no.nav.tilleggsstonader.sak.googlemaps.placeDetailsApi.PlaceDetailsResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-google-place-details")
class GooglePlaceDetailsClientMockConfig {
    @Bean
    @Primary
    fun googlePlaceDetailsClient() = mockk<GooglePlaceDetailsClient>().apply { resetTilDefault(this) }

    companion object {
        fun resetTilDefault(client: GooglePlaceDetailsClient) {
            clearMocks(client)
            every { client.finnStedDetaljer(any()) } answers {
                val placeId = firstArg<String>()
                PlaceDetailsResponse(
                    id = placeId,
                    formattedAddress = "Mock address for $placeId",
                    displayName = null,
                )
            }
        }
    }
}
