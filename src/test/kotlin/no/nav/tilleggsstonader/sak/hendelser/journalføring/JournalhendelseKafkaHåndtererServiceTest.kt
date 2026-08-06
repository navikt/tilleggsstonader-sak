package no.nav.tilleggsstonader.sak.hendelser.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.sak.ekstern.journalføring.HåndterMottattKjørelisteService
import no.nav.tilleggsstonader.sak.ekstern.journalføring.HåndterSøknadService
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.util.dokumentInfoMedOriginalVariant
import no.nav.tilleggsstonader.sak.util.journalpost
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class JournalhendelseKafkaHåndtererServiceTest {
    private val journalpostService = mockk<JournalpostService>()
    private val håndterSøknadService = mockk<HåndterSøknadService>(relaxed = true)
    private val journalpostMottattMetrikker = mockk<JournalpostMottattMetrikker>(relaxed = true)
    private val håndterMottattKjørelisteService = mockk<HåndterMottattKjørelisteService>(relaxed = true)

    private val service =
        JournalhendelseKafkaHåndtererService(
            journalpostService = journalpostService,
            håndterSøknadService = håndterSøknadService,
            journalpostMottattMetrikker = journalpostMottattMetrikker,
            håndterMottattKjørelisteService = håndterMottattKjørelisteService,
        )

    @ParameterizedTest
    @EnumSource(
        value = DokumentBrevkode::class,
        names = [
            "STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_ELLER_HJEMREISE",
            "STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_ELLER_HJEMREISE_GAMMEL",
            "REISE_FOR_Å_KOMME_I_ARBEID",
            "REISE_FOR_Å_KOMME_I_ARBEID_GAMMEL",
            "FLYTTING",
            "FLYTTING_GAMMEL",
        ],
    )
    fun `skal ikke prosessere nye brevkoder for reise og flytting`(brevkode: DokumentBrevkode) {
        val journalpost = journalpost(dokumenter = listOf(dokumentInfoMedOriginalVariant(brevkode)))
        every { journalpostService.hentJournalpost(journalpost.journalpostId) } returns journalpost

        service.behandleJournalhendelse(journalpost.journalpostId)

        verify(exactly = 0) { håndterSøknadService.håndterSøknad(any()) }
        verify(exactly = 0) { håndterMottattKjørelisteService.behandleKjøreliste(any()) }
    }

    @ParameterizedTest
    @EnumSource(
        value = DokumentBrevkode::class,
        names = [
            "LÆREMIDLER",
            "BOUTGIFTER",
            "PASS_AV_BARN",
            "DAGLIG_REISE",
            "REISE_TIL_SAMLING",
        ],
    )
    fun `skal prosessere eksisterende søknadsbrevkoder`(brevkode: DokumentBrevkode) {
        val journalpost = journalpost(dokumenter = listOf(dokumentInfoMedOriginalVariant(brevkode)))
        every { journalpostService.hentJournalpost(journalpost.journalpostId) } returns journalpost

        service.behandleJournalhendelse(journalpost.journalpostId)

        verify(exactly = 1) { håndterSøknadService.håndterSøknad(journalpost) }
    }

    @ParameterizedTest
    @EnumSource(value = DokumentBrevkode::class, names = ["DAGLIG_REISE_KJØRELISTE"])
    fun `skal prosessere eksisterende kjørelistebrevkode`(brevkode: DokumentBrevkode) {
        val journalpost =
            journalpost(
                dokumenter =
                    listOf(
                        dokumentInfoMedOriginalVariant(brevkode),
                    ),
            )
        every { journalpostService.hentJournalpost(journalpost.journalpostId) } returns journalpost

        service.behandleJournalhendelse(journalpost.journalpostId)

        verify(exactly = 1) { håndterMottattKjørelisteService.behandleKjøreliste(journalpost) }
    }
}
