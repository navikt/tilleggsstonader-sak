package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.brev.FamilieDokumentClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-familie-dokument")
class FamilieDokumentClientConfig {

    @Bean
    @Primary
    fun familieDokumentClient(): FamilieDokumentClient {
        val client = mockk<FamilieDokumentClient>()
        every { client.genererPdf(any()) } returns byteArrayOf()
        return client
    }
}