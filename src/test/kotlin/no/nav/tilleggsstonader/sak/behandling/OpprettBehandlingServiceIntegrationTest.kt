package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.vent.SettPåVentService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.assertFinnesTaskMedType
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class OpprettBehandlingServiceIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var opprettBehandlingService: OpprettBehandlingService

    @Autowired
    private lateinit var settPåVentService: SettPåVentService

    private val behandlingÅrsak = BehandlingÅrsak.SØKNAD

    private val opprettBehandlingOppgaveMetadata =
        OpprettBehandlingOppgaveMetadata.OppgaveMetadata(
            tilordneSaksbehandler = "nissemann",
            beskrivelse = "Ny oppgave",
            prioritet = OppgavePrioritet.NORM,
        )

    @Test
    internal fun `skal feile hvis krav mottatt er frem i tid`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())

        assertThatExceptionOfType(ApiFeil::class.java)
            .isThrownBy {
                opprettBehandlingService.opprettBehandling(
                    OpprettBehandling(
                        fagsakId = fagsak.id,
                        stegType = StegType.VILKÅR,
                        behandlingsårsak = BehandlingÅrsak.PAPIRSØKNAD,
                        kravMottatt = LocalDate.now().plusDays(1),
                        oppgaveMetadata = opprettBehandlingOppgaveMetadata,
                    ),
                )
            }.withMessage("Kan ikke sette krav mottattdato frem i tid")
    }

    @Test
    internal fun `kaster feil om forrige behandling ikke er ferdigstilt når tillatFlereÅpneBehandlinger=false`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        testoppsettService.lagre(
            behandling(
                fagsak = fagsak,
                status = BehandlingStatus.UTREDES,
            ),
        )
        assertThatExceptionOfType(Feil::class.java)
            .isThrownBy {
                opprettBehandlingService.opprettBehandling(
                    OpprettBehandling(
                        fagsak.id,
                        behandlingsårsak = behandlingÅrsak,
                        oppgaveMetadata = opprettBehandlingOppgaveMetadata,
                        tillatFlereÅpneBehandlinger = false,
                    ),
                )
            }.withMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
    }

    @Nested
    inner class BehandlingPåVent {
        @Test
        internal fun `opprettBehandling av førstegangsbehandling er mulig hvis det finnes en førstegangsbehandling på vent`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT))

            val nyBehandling =
                opprettBehandlingService.opprettBehandling(
                    OpprettBehandling(
                        fagsak.id,
                        behandlingsårsak = behandlingÅrsak,
                        oppgaveMetadata = opprettBehandlingOppgaveMetadata,
                        tillatFlereÅpneBehandlinger = true,
                    ),
                )

            assertThat(nyBehandling.status).isEqualTo(BehandlingStatus.OPPRETTET)
            assertFinnesTaskMedType(OpprettOppgaveForOpprettetBehandlingTask.TYPE)
        }

        @Test
        internal fun `opprettBehandling av førstegangsbehandling er mulig hvis det finnes en åpen førstegangsbehandling`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(behandling(fagsak, BehandlingStatus.OPPRETTET))

            // Sjekker at denne ikke kaster feil
            val nyBehandling =
                opprettBehandlingService.opprettBehandling(
                    OpprettBehandling(
                        fagsak.id,
                        behandlingsårsak = behandlingÅrsak,
                        oppgaveMetadata = opprettBehandlingOppgaveMetadata,
                        tillatFlereÅpneBehandlinger = true,
                    ),
                )

            // For å opprette oppgave slik at ikke settPåVentService-kall feiler
            kjørTasksKlareForProsessering()
            assertThat(nyBehandling.status).isEqualTo(BehandlingStatus.SATT_PÅ_VENT)
            assertThat(settPåVentService.hentStatusSettPåVent(nyBehandling.id)).isNotNull
        }

        @Test
        internal fun `opprettBehandling av revurdering er mulig hvis det finnes en førstegangsbehandling på vent`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            val førstegangsbehandling = testoppsettService.lagre(behandling(fagsak))
            testoppsettService.ferdigstillBehandling(førstegangsbehandling)

            testoppsettService.lagre(behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT, type = BehandlingType.REVURDERING))

            val nyBehandling =
                opprettBehandlingService.opprettBehandling(
                    OpprettBehandling(
                        fagsak.id,
                        behandlingsårsak = behandlingÅrsak,
                        oppgaveMetadata = opprettBehandlingOppgaveMetadata,
                        tillatFlereÅpneBehandlinger = true,
                    ),
                )

            assertThat(nyBehandling.status).isEqualTo(BehandlingStatus.OPPRETTET)
        }

        @Test
        internal fun `opprettBehandling av revurdering er mulig hvis det finnes en åpen førstegangsbehandling`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            val førstegangsbehandling = testoppsettService.lagre(behandling(fagsak))
            testoppsettService.ferdigstillBehandling(førstegangsbehandling)

            testoppsettService.lagre(behandling(fagsak, BehandlingStatus.OPPRETTET, type = BehandlingType.REVURDERING))

            val nyBehandling =
                opprettBehandlingService.opprettBehandling(
                    OpprettBehandling(
                        fagsak.id,
                        behandlingsårsak = behandlingÅrsak,
                        oppgaveMetadata = opprettBehandlingOppgaveMetadata,
                        tillatFlereÅpneBehandlinger = true,
                    ),
                )

            // For å opprette oppgave slik at ikke settPåVentService-kall feiler
            assertFinnesTaskMedType(OpprettOppgaveForOpprettetBehandlingTask.TYPE)
            kjørTasksKlareForProsessering()
            assertThat(nyBehandling.status).isEqualTo(BehandlingStatus.SATT_PÅ_VENT)
            assertThat(settPåVentService.hentStatusSettPåVent(nyBehandling.id)).isNotNull
        }
    }

    @Test
    internal fun `opprettBehandling er mulig hvis det finnes en revurdering på vent`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        testoppsettService.lagre(behandling(fagsak, BehandlingStatus.FERDIGSTILT))
        testoppsettService.lagre(
            behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT, type = BehandlingType.REVURDERING),
        )

        val nyBehandling =
            opprettBehandlingService.opprettBehandling(
                OpprettBehandling(
                    fagsak.id,
                    behandlingsårsak = behandlingÅrsak,
                    oppgaveMetadata = opprettBehandlingOppgaveMetadata,
                    tillatFlereÅpneBehandlinger = true,
                ),
            )

        assertThat(nyBehandling.status).isEqualTo(BehandlingStatus.OPPRETTET)
        assertFinnesTaskMedType(OpprettOppgaveForOpprettetBehandlingTask.TYPE)
    }

    @Test
    internal fun `opprettBehandling med oppgaveMetadata=UtenOppgave skal ikke opprette oppgave`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())

        opprettBehandlingService.opprettBehandling(
            OpprettBehandling(
                fagsak.id,
                behandlingsårsak = behandlingÅrsak,
                oppgaveMetadata = OpprettBehandlingOppgaveMetadata.UtenOppgave,
                tillatFlereÅpneBehandlinger = true,
            ),
        )

        assertFinnesTaskMedType(OpprettOppgaveForOpprettetBehandlingTask.TYPE, antall = 0)
    }
}
