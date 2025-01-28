package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.interntVedtak.HtmlifyClient
import no.nav.tilleggsstonader.sak.interntVedtak.InterntVedtak
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-htmlify")
class HtmlifyClientConfig {
    @Bean
    @Primary
    fun htmlifyClient(): HtmlifyClient {
        val client = mockk<HtmlifyClient>()
        every { client.generateHtml(any<InterntVedtak>()) } returns "<body>body</body>"
        return client
    }
}
