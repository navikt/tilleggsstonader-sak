package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.ArenaKontraktUtil.aktivitetArenaDto
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-register-aktivitet")
class RegisterAktivitetClientMockConfig {
    @Bean
    @Primary
    fun registerAktivitetClient() = mockk<RegisterAktivitetClient>().apply { resetTilDefault(this) }

    companion object {
        fun resetTilDefault(client: RegisterAktivitetClient) {
            clearMocks(client)
            every { client.hentAktiviteter(any(), any(), any()) } returns listOf(aktivitetArenaDto())
        }
    }
}
