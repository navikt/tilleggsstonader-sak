package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveClient
import org.springframework.stereotype.Service

/**
 * Service for å enklere tilbakestille mocks i tester, og er eksponert i [no.nav.tilleggsstonader.sak.IntegrationTest]
 */
@Service
class MockService(
    val oppgaveClient: OppgaveClient,
    val oppgavelager: Oppgavelager,
    val journalpostClient: JournalpostClient,
) {
    fun resetJournalpostClient() {
        JournalpostClientConfig.resetMock(journalpostClient)
    }

    fun resetOppgaveClient() {
        OppgaveClientConfig.resetMock(oppgaveClient, oppgavelager = oppgavelager)
    }
}
