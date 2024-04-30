package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingClient
import no.nav.tilleggsstonader.sak.arbeidsfordeling.Arbeidsfordelingsenhet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-arbeidsfordeling")
class ArbeidsfordelingClientConfig {

    @Bean
    @Primary
    fun arbedisfordelingClient(): ArbeidsfordelingClient {
        val client = mockk<ArbeidsfordelingClient>()

        every { client.finnArberidsfordelingsenhet(any()) } returns listOf(Arbeidsfordelingsenhet("4462", "NAY Nasjonal"))

        return client
    }
}
