package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.clearMocks
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
class HtmlifyClientMockConfig {
    @Bean
    @Primary
    fun htmlifyClient() = mockk<HtmlifyClient>().apply { resetTilDefault(this) }

    companion object {
        fun resetTilDefault(client: HtmlifyClient) {
            clearMocks(client)
            every { client.generateHtml(any<InterntVedtak>()) } returns "<body>body</body>"
        }
    }
}
