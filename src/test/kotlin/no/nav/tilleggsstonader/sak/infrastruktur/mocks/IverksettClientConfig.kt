package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.iverksett.IverksettClient
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.BeriketSimuleringsresultat
import no.nav.tilleggsstonader.sak.util.FileUtil.readFile
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-iverksett")
class IverksettClientConfig {

    @Bean
    @Primary
    fun iverksettClient(): IverksettClient {
        val iverksettClient = mockk<IverksettClient>(relaxed = true)
        clearMock(iverksettClient)
        return iverksettClient
    }

    companion object {

        private val simuleringsresultat = objectMapper.readValue<BeriketSimuleringsresultat>(
            readFile("mock/iverksett/simuleringsresultat_beriket.json"),
        )

        fun clearMock(iverksettClient: IverksettClient) {
            clearMocks(iverksettClient)
            every { iverksettClient.simuler(any()) } returns simuleringsresultat
        }
    }
}
