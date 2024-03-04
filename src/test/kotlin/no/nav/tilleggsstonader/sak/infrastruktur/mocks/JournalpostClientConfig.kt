package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariant
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestClientException

@Configuration
@Profile("mock-journalpost")
class JournalpostClientConfig {

    private val dummyPdf = javaClass.classLoader.getResource("dummy/dummy.pdf")!!.readBytes()

    @Bean
    @Primary
    fun journalpostClient(): JournalpostClient {
        val journalpostClient = mockk<JournalpostClient>()

        every { journalpostClient.distribuerJournalpost(any(), any()) } returns "bestillingId"
        every {
            journalpostClient.opprettJournalpost(any(), any())
        } returns ArkiverDokumentResponse(journalpostId = "journalpostId", ferdigstilt = true)
        every { journalpostClient.hentJournalpost(any()) } answers { mockJournalpost(firstArg()) }
        every { journalpostClient.hentDokument(any(), any(), any()) } returns dummyPdf
        mockFeiletDistribusjon(journalpostClient)

        return journalpostClient
    }

    /**
     * Returnerer en journalpost som returnerer et dokument for å kunne hente vedlegg fra frontend
     */
    private fun mockJournalpost(journalpostId: String): Journalpost {
        return Journalpost(
            journalpostId,
            Journalposttype.I,
            Journalstatus.FERDIGSTILT,
            bruker = Bruker("ident", BrukerIdType.FNR),
            dokumenter = listOf(
                DokumentInfo(
                    // Samme id som i [OpprettTestBehandlingController] for å kunne hente vedlegg lokalt
                    "0a53867a-3d6e-4947-b5de-9578ecbdf03d",
                    dokumentvarianter = listOf(Dokumentvariant(Dokumentvariantformat.ARKIV, null, true)),
                ),
            ),
        )
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
