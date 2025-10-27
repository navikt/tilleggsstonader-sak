package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettClient
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettStatus
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringResponseDto
import no.nav.tilleggsstonader.sak.util.FileUtil.readFile
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-iverksett")
class IverksettClientMockConfig {
    @Bean
    @Primary
    fun iverksettClient() = mockk<IverksettClient>(relaxed = true).apply { resetTilDefault(this) }

    companion object {
        fun resetTilDefault(iverksettClient: IverksettClient) {
            clearMocks(iverksettClient)
            justRun { iverksettClient.iverksett(any()) }
            every { iverksettClient.hentStatus(any(), any(), any()) } returns IverksettStatus.OK
            every { iverksettClient.simulerV2(any()) } returns simuleringsresultat
            every { iverksettClient.simulerV3(any()) } returns simuleringsresultat
            every { iverksettClient.simulerV2(match { it.personident == "identIngenEndring" }) } returns null
        }

        private val simuleringsresultat =
            objectMapper.readValue<SimuleringResponseDto>(
                readFile("mock/iverksett/simuleringsresultat.json"),
            )
    }
}
