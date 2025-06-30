package no.nav.tilleggsstonader.sak.hendelser.journalføring

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.sak.ekstern.journalføring.HåndterSøknadService
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class JournalhendelseKafkaHåndtererServiceTest {
    val journalpostService = mockk<JournalpostService>()
    val håndterSøknadService = mockk<HåndterSøknadService>()

    val service =
        JournalhendelseKafkaHåndtererService(
            journalpostService = journalpostService,
            håndterSøknadService = håndterSøknadService,
        )

    @BeforeEach
    fun setUp() {
        justRun { håndterSøknadService.håndterSøknad(any(), any()) }
    }

    @Nested
    inner class Boutgifter {
        @Test
        fun `skal håndtere innkommende journalhendelse for boutgifter sendt inn via nav-no`() {
            val journalpost =
                journalpost(
                    journalposttype = Journalposttype.I,
                    brevkode = DokumentBrevkode.BOUTGIFTER.verdi,
                    kanal = Kanal.NAV_NO,
                )
            every { journalpostService.hentJournalpost(any()) } returns journalpost

            service.behandleJournalhendelse(journalpost.journalpostId)

            verify(exactly = 1) { håndterSøknadService.håndterSøknad(journalpost, Stønadstype.BOUTGIFTER) }
        }

        @Test
        fun `skal håndtere innkommende journalhendelse for boutgifter sendt inn via skanning`() {
            val journalpost =
                journalpost(
                    journalposttype = Journalposttype.I,
                    brevkode = DokumentBrevkode.BOUTGIFTER.verdi,
                    kanal = Kanal.SKAN_NO,
                )
            every { journalpostService.hentJournalpost(any()) } returns journalpost

            service.behandleJournalhendelse(journalpost.journalpostId)

            verify(exactly = 1) { håndterSøknadService.håndterSøknad(journalpost, Stønadstype.BOUTGIFTER) }
        }
    }

    @Test
    fun `skal ignorere innkommende journalhendelse for tilsyn barn då de håndteres via søknad-api`() {
        val journalpost =
            journalpost(
                journalposttype = Journalposttype.I,
                brevkode = DokumentBrevkode.BARNETILSYN.verdi,
                kanal = Kanal.SKAN_NO,
            )
        every { journalpostService.hentJournalpost(any()) } returns journalpost

        service.behandleJournalhendelse(journalpost.journalpostId)

        verify(exactly = 0) { håndterSøknadService.håndterSøknad(any(), any()) }
    }

    private fun journalpost(
        journalposttype: Journalposttype = Journalposttype.I,
        brevkode: String? = null,
        kanal: Kanal = Kanal.SKAN_NO,
        tema: Tema = Tema.TSO,
    ) = Journalpost(
        journalpostId = "1",
        journalposttype = journalposttype,
        journalstatus = Journalstatus.MOTTATT,
        dokumenter =
            listOf(
                DokumentInfo(
                    dokumentInfoId = "1",
                    tittel = "Testdokument",
                    brevkode = brevkode,
                ),
            ),
        tema = tema.name,
        kanal = kanal.name,
    )

    enum class Kanal {
        NAV_NO,
        SKAN_NO,
        ANNEN_KODE,
    }
}
