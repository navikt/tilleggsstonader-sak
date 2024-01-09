package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestClientException

@Configuration
@Profile("mock-journalpost")
class JournalpostClientConfig {

    @Bean
    @Primary
    fun journalpostClient(): JournalpostClient {
        val journalpostClient = mockk<JournalpostClient>()

        every { journalpostClient.distribuerJournalpost(any(), any()) } returns "bestillingId"
        every {
            journalpostClient.opprettJournalpost(
                any(),
                any(),
            )
        } returns ArkiverDokumentResponse(journalpostId = "journalpostId", ferdigstilt = true)
        mockFeiletDistribusjon(journalpostClient)

        return journalpostClient
    }

    private fun mockFeiletDistribusjon(journalpostClient: JournalpostClient) {
        every {
            journalpostClient.distribuerJournalpost(
                match { it.journalpostId == journalpostIdMedFeil },
                any(),
            )
        } throws RestClientException("noe feilet")
    }

    companion object {
        const val journalpostIdMedFeil = "journalpostIdMedFeil"
    }
}
