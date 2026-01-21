package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.clearMocks
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.GoogleRoutesClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-google-routes")
class GoogleRoutesClientMockConfig {
    @Bean
    @Primary
    fun googleRoutesClient() = mockk<GoogleRoutesClient>().apply { resetTilDefault(this) }

    companion object {
        fun resetTilDefault(client: GoogleRoutesClient) {
            clearMocks(client)
        }
    }
}
