package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.tilleggsstonader.kontrakter.dokarkiv.OppdaterJournalpostResponse
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.AvsenderMottaker
import no.nav.tilleggsstonader.kontrakter.journalpost.AvsenderMottakerIdType
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariant
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.JournalposterForBrukerRequest
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.journalpost.LogiskVedlegg
import no.nav.tilleggsstonader.kontrakter.journalpost.RelevantDato
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestClientException
import java.time.LocalDateTime

@Configuration
@Profile("mock-journalpost")
class JournalpostClientMockConfig {
    @Bean
    @Primary
    fun journalpostClient() = mockk<JournalpostClient>().apply { resetTilDefault(this) }

    companion object {
        fun resetTilDefault(journalpostClient: JournalpostClient) {
            clearMocks(journalpostClient)
            resetJournalposter()

            every { journalpostClient.hentJournalpost(any()) } answers {
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
            every { journalpostClient.hentDokument(any(), any(), eq(Dokumentvariantformat.ARKIV)) } returns dummyPdf
            every { journalpostClient.oppdaterJournalpost(any(), any(), any()) } answers {
                val journalpostId = secondArg<String>()
                OppdaterJournalpostResponse(journalpostId)
            }
            every { journalpostClient.ferdigstillJournalpost(any(), any(), any()) } answers {
                val journalpostId = firstArg<String>()
                OppdaterJournalpostResponse(journalpostId)
            }
            mockFeiletDistribusjon(journalpostClient)

            every { journalpostClient.ferdigstillJournalpost(any(), any(), any()) } returns mockk()
            every { journalpostClient.oppdaterJournalpost(any(), any(), any()) } returns mockk()
            every { journalpostClient.oppdaterLogiskeVedlegg(any(), any()) } answers { firstArg() }
            every { journalpostClient.finnJournalposterForBruker(any()) } answers {
                journalposter.values.filter { it.bruker?.id == firstArg<JournalposterForBrukerRequest>().brukerId.id } +
                    listOf(
                        Journalpost(
                            "2",
                            Journalposttype.I,
                            journalstatus = Journalstatus.MOTTATT,
                            tema = Tema.TSO.toString(),
                            behandlingstema = "ab0300",
                            tittel = "Søknad om barnetilsyn",
                            bruker = Bruker("12345678910", BrukerIdType.FNR),
                            avsenderMottaker = avsenderMottaker(),
                            journalforendeEnhet = "tilleggsstonader-sak",
                            relevanteDatoer =
                                listOf(
                                    RelevantDato(LocalDateTime.now().minusDays(7), "DATO_REGISTRERT"),
                                    RelevantDato(LocalDateTime.now(), "DATO_JOURNALFOERT"),
                                ),
                            dokumenter =
                                listOf(
                                    DokumentInfo(
                                        dokumentInfoId = "1",
                                        tittel = "Dummy dokument 1",
                                        logiskeVedlegg =
                                            listOf(
                                                LogiskVedlegg("1", "Dokumentasjon på sykdom"),
                                                LogiskVedlegg("2", "Inntektsendring"),
                                                LogiskVedlegg("3", "Samværsmelding"),
                                            ),
                                        dokumentvarianter =
                                            listOf(
                                                Dokumentvariant(
                                                    variantformat = Dokumentvariantformat.ARKIV,
                                                    saksbehandlerHarTilgang = true,
                                                ),
                                            ),
                                    ),
                                    DokumentInfo(
                                        dokumentInfoId = "2",
                                        tittel = "Dummy dokument 2",
                                        dokumentvarianter =
                                            listOf(
                                                Dokumentvariant(
                                                    variantformat = Dokumentvariantformat.ARKIV,
                                                    saksbehandlerHarTilgang = true,
                                                ),
                                            ),
                                    ),
                                ),
                        ),
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
                            relevanteDatoer =
                                listOf(
                                    RelevantDato(LocalDateTime.now().minusDays(7), "DATO_REGISTRERT"),
                                    RelevantDato(LocalDateTime.now(), "DATO_JOURNALFOERT"),
                                ),
                            dokumenter =
                                listOf(
                                    DokumentInfo(
                                        dokumentInfoId = "3",
                                        tittel = "Dummy dokument 3",
                                        logiskeVedlegg =
                                            listOf(
                                                LogiskVedlegg("1", "Dokumentasjon på sykdom"),
                                                LogiskVedlegg("2", "Inntektsendring"),
                                                LogiskVedlegg("3", "Samværsmelding"),
                                            ),
                                        dokumentvarianter =
                                            listOf(
                                                Dokumentvariant(
                                                    variantformat = Dokumentvariantformat.ARKIV,
                                                    saksbehandlerHarTilgang = true,
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    )
            }
        }

        val journalposter = mutableMapOf<Long, Journalpost>()

        const val JOURNALPOST_ID_MED_FEIL = "journalpostIdMedFeil"

        private val dummyPdf =
            this::class.java.classLoader
                .getResource("interntVedtak/BARNETILSYN/internt_vedtak.pdf")!!
                .readBytes()

        private fun avsenderMottaker() =
            AvsenderMottaker(
                id = "12345678910",
                type = AvsenderMottakerIdType.FNR,
                navn = "Ola Nordmann",
                land = "NOR",
                erLikBruker = true,
            )

        private fun mockFeiletDistribusjon(journalpostClient: JournalpostClient) {
            every {
                journalpostClient.distribuerJournalpost(
                    match { it.journalpostId == JOURNALPOST_ID_MED_FEIL },
                    any(),
                )
            } throws RestClientException("noe feilet")
        }

        private fun resetJournalposter() {
            journalposter.clear()
            journalposter[journalpost.journalpostId.toLong()] = journalpost
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
                relevanteDatoer =
                    listOf(
                        RelevantDato(LocalDateTime.now().minusDays(7), "DATO_REGISTRERT"),
                        RelevantDato(LocalDateTime.now(), "DATO_JOURNALFOERT"),
                    ),
                dokumenter =
                    listOf(
                        DokumentInfo(
                            dokumentInfoId = "1",
                            tittel = "Dummy dokument 1",
                            logiskeVedlegg =
                                listOf(
                                    LogiskVedlegg("1", "Dokumentasjon på sykdom"),
                                    LogiskVedlegg("2", "Inntektsendring"),
                                    LogiskVedlegg("3", "Samværsmelding"),
                                ),
                            dokumentvarianter =
                                listOf(
                                    Dokumentvariant(
                                        variantformat = Dokumentvariantformat.ARKIV,
                                        saksbehandlerHarTilgang = true,
                                    ),
                                ),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "2",
                            tittel = "Dummy dokument 2",
                            dokumentvarianter =
                                listOf(
                                    Dokumentvariant(
                                        variantformat = Dokumentvariantformat.ARKIV,
                                        saksbehandlerHarTilgang = true,
                                    ),
                                ),
                        ),
                    ),
            )
    }
}
