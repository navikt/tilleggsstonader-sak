package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.opplysninger.fullmakt.FullmaktClient
import no.nav.tilleggsstonader.sak.util.FullmektigStubs
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-fullmakt")
class FullmaktClientMockConfig {
    @Bean
    @Primary
    fun fullmaktClient() = mockk<FullmaktClient>().apply { resetTilDefault(this) }

    companion object {
        fun resetTilDefault(fullmaktClient: FullmaktClient) {
            clearMocks(fullmaktClient)
            every { fullmaktClient.hentFullmektige(any()) } returns listOf(FullmektigStubs.gyldig)
        }
    }
}
