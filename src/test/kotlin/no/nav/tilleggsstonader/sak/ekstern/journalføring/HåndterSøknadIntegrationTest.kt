package no.nav.tilleggsstonader.sak.ekstern.journalføring

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførHenleggelse
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.defaultJournalpost
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HåndterSøknadIntegrationTest : CleanDatabaseIntegrationTest() {
    @Test
    internal fun `skal kunne automatisk journalføre hvis det ikke finnes eksisterende sak på person`() {
        val opprettetBehandling = håndterSøknadService.håndterSøknad(defaultJournalpost)

        assertThat(opprettetBehandling).isNotNull()
    }

    @Test
    fun `skal opprette ny behandling satt på vent hvis det allerede finnes aktiv behandling`() {
        // Første journalføring vil føre til at det opprettes behandling
        håndterSøknadService.håndterSøknad(defaultJournalpost)
        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        // Andre journalføring skal føre til ny behandling satt på vent
        val nyBehandling = håndterSøknadService.håndterSøknad(defaultJournalpost)

        assertThat(nyBehandling).isNotNull()
        assertThat(nyBehandling!!.status).isEqualTo(BehandlingStatus.SATT_PÅ_VENT)
    }

    @Test
    internal fun `oppretter behandling hvis alle eksisterende behandlinger er ferdigstilt`() {
        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReiseTsoTestdata()
        }

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
