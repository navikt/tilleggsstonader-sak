package no.nav.tilleggsstonader.sak.ekstern.journalføring

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteService
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class HåndterMottattKjørelisteServiceTest {
    private val kjørelisteService = mockk<KjørelisteService>()
    private val service =
        HåndterMottattKjørelisteService(
            journalpostClient = mockk(),
            dagligReisePrivatBilService = mockk(),
            behandlingService = mockk(),
            journalpostService = mockk(),
            fagsakService = mockk(),
            kjørelisteService = kjørelisteService,
            taskService = mockk(),
        )

    @Test
    fun `skal kaste feil om kjøreliste med samme journalpostId allerede eksisterer`() {
        val journalpostId = "duplikat-jp-123"
        val journalpost = mockk<Journalpost>()
        every { journalpost.journalpostId } returns journalpostId
        every { kjørelisteService.eksistererForJournalpostId(journalpostId) } returns true

        assertThatThrownBy { service.behandleKjøreliste(journalpost) }
            .hasMessage("Kjøreliste med journalpostId=${journalpost.journalpostId} er allerede mottatt")
    }
}
