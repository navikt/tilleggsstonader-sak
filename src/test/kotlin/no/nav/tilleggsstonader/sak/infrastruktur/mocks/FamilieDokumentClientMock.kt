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
class FamilieDokumentClientMock {
    @Bean
    @Primary
    fun familieDokument(): FamilieDokumentClient {
        val familieDokumentClient: FamilieDokumentClient = mockk()
        val dummyPdf =
            this::class.java.classLoader
                .getResource("dummy/pdf_dummy.pdf")!!
                .readBytes()
        every { familieDokumentClient.genererPdf(any()) } returns dummyPdf
        return familieDokumentClient
    }
}
