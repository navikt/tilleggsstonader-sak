package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.nvdbApi.NvdbBomstasjon
import no.nav.tilleggsstonader.sak.nvdbApi.NvdbBomstasjonClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.test.util.AopTestUtils

@Configuration
@Profile("mock-nvdb")
class NvdbBomstasjonClientMockConfig {
    @Bean
    @Primary
    fun nvdbBomstasjonClient() = mockk<NvdbBomstasjonClient>().apply { resetTilDefault(this) }

    companion object {
        fun resetTilDefault(client: NvdbBomstasjonClient) {
            // NvdbBomstasjonClient har @Cacheable-metode, så Spring kan wrappe beanen i en proxy.
            // Derfor henter vi ut underliggende mock før MockK-operasjoner.
            val faktiskClient = runCatching { AopTestUtils.getTargetObject<NvdbBomstasjonClient>(client) }.getOrElse { client }

            clearMocks(faktiskClient)
            every { faktiskClient.hentAlleBomstasjoner() } returns emptyList<NvdbBomstasjon>()
        }
    }
}
