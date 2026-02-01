package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.SimuleringClient
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringResponseDto
import no.nav.tilleggsstonader.sak.util.FileUtil.readFile
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import tools.jackson.module.kotlin.readValue

@Configuration
@Profile("mock-iverksett")
class IverksettClientMockConfig {
    @Bean
    @Primary
    fun iverksettClient() = mockk<SimuleringClient>(relaxed = true).apply { resetTilDefault(this) }

    companion object {
        fun resetTilDefault(simuleringClient: SimuleringClient) {
            clearMocks(simuleringClient)
            every { simuleringClient.simuler(any()) } returns simuleringsresultat
        }

        private val simuleringsresultat =
            jsonMapper.readValue<SimuleringResponseDto>(
                readFile("mock/iverksett/simuleringsresultat.json"),
            )
    }
}
