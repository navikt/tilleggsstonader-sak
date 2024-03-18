package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.*
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

        val journalposter: MutableMap<Long, Journalpost> = listOf(journalpost).associateBy { it.journalpostId.toLong() }
            .toMutableMap()

        every { journalpostClient.hentJournalpost(any())} answers {
            val journalpostId = firstArg<String>()
            journalposter[journalpostId.toLong()] ?: error("Finner ikke journalpost med id=$journalpostId")
        }
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

    private val journalpost =
        Journalpost(
            "1",
            Journalposttype.I,
            journalstatus = Journalstatus.MOTTATT,
            tema = Tema.TSO.toString(),
            behandlingstema = "ab0300",
            tittel = "Søknad om barnetilsyn",
            bruker = Bruker("12345678910", BrukerIdType.FNR),
            avsenderMottaker = avsenderMottaker(),
            journalforendeEnhet = "tilleggsstonader-sak",
        )

    private fun avsenderMottaker() = AvsenderMottaker(
        id = "12345678910",
        type = AvsenderMottakerIdType.FNR,
        navn = "Ola Nordmann",
        land = "NOR",
        erLikBruker = true
    )

    companion object {
        const val journalpostIdMedFeil = "journalpostIdMedFeil"
    }
}
