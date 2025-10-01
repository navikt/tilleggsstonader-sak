package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.journalføring.FamilieDokumentClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-familie-dokument")
class FamilieDokumentClientMockConfig {
    @Bean
    @Primary
    fun familieDokument() = mockk<FamilieDokumentClient>().apply { resetTilDefault(this) }

    companion object {
        fun resetTilDefault(client: FamilieDokumentClient) {
            val dummyPdf =
                this::class.java.classLoader
                    .getResource("dummy/pdf_dummy.pdf")!!
                    .readBytes()
            every { client.genererPdf(any()) } returns dummyPdf
        }
    }
}
