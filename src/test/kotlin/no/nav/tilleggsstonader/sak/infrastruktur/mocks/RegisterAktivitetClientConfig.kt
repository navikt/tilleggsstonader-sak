package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetClient
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.ArenaKontraktUtil.aktivitetArenaDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-register-aktivitet")
class RegisterAktivitetClientConfig {

    @Bean
    @Primary
    fun registerAktivitetClient(): RegisterAktivitetClient {
        val client = mockk<RegisterAktivitetClient>()
        resetMock(client)
        return client
    }

    companion object {
        fun resetMock(client: RegisterAktivitetClient) {
            clearMocks(client)
            every { client.hentAktiviteter(any(), any(), any()) } returns listOf(aktivitetArenaDto())
        }
    }
}
