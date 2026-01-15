package no.nav.tilleggsstonader.sak.ekstern.journalføring

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.integrasjonstest.defaultJournalpost
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.finnAlleTaskerMedType
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførHenleggelse
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HåndterSøknadIntegrationTest : CleanDatabaseIntegrationTest() {
    @Test
    internal fun `skal kunne automatisk journalføre hvis det ikke finnes eksisterende sak på person`() {
        val opprettetBehandling = håndterSøknadService.håndterSøknad(defaultJournalpost)

        assertThat(opprettetBehandling).isNotNull()
    }

    @Test
    fun `skal opprette journalføringsoppgave hvis det allerede finnes aktiv behandling`() {
        // Første journalføring vil føre til at det opprettes behandling
        val journalpostId = "1"
        håndterSøknadService.håndterSøknad(defaultJournalpost.copy(journalpostId = journalpostId))
        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        // Andre journalføring skal føre til journalføringsoppgave
        håndterSøknadService.håndterSøknad(defaultJournalpost)

        val oppgaveTask = finnAlleTaskerMedType(OpprettOppgaveTask.TYPE).single()
        val payload = objectMapper.readValue<OpprettOppgaveTask.OpprettOppgaveTaskData>(oppgaveTask.payload)

        assertThat(payload.oppgave.journalpostId).isEqualTo(journalpostId)
        assertThat(payload.oppgave.oppgavetype).isEqualTo(Oppgavetype.Journalføring)
    }

    @Test
    internal fun `oppretter behandling hvis alle eksisterende behandlinger er ferdigstilt`() {
        opprettBehandlingOgGjennomførBehandlingsløp()

        val nyBehandling = håndterSøknadService.håndterSøknad(defaultJournalpost)
        assertThat(nyBehandling).isNotNull()
    }

    @Test
    fun `kan ikke automatisk journalføre hvis kanal er skanning`() {
        val journalpostSkanning = defaultJournalpost.copy(kanal = "SKAN_IM")
        val behandling = håndterSøknadService.håndterSøknad(journalpostSkanning)
        assertThat(behandling).isNull()
    }

    @Test
    internal fun `kan opprette behandling hvis alle behandlinger i ny løsning er henlagt`() {
        gjennomførHenleggelse()

        val nyBehandling = håndterSøknadService.håndterSøknad(defaultJournalpost)
        assertThat(nyBehandling).isNotNull()
    }
}
