package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.opplysninger.egenansatt.EgenAnsattClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-egen-ansatt")
class EgenAnsattConfig {

    @Bean
    @Primary
    fun egenAnsattClient(): EgenAnsattClient {
        val client = mockk<EgenAnsattClient>()
        every { client.erEgenAnsatt(any<Set<String>>()) } answers {
            firstArg<Set<String>>().associateWith { it == "ikkeTilgang" }
        }
        every { client.erEgenAnsatt(any<String>()) } answers { firstArg<String>() == "ikkeTilgang" }
        return client
    }
}
