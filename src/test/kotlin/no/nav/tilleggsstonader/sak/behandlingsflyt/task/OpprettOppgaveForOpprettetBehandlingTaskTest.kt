package no.nav.tilleggsstonader.sak.behandlingsflyt.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveMappe
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class OpprettOppgaveForOpprettetBehandlingTaskTest {
    val behandlingService = mockk<BehandlingService>()
    val oppgaveService = mockk<OppgaveService>()
    val opprettOppgaveForOpprettetBehandlingTask =
        OpprettOppgaveForOpprettetBehandlingTask(
            behandlingService = behandlingService,
            oppgaveService = oppgaveService,
        )

    val oppgaveId = 1L

    val opprettOppgaveSlot = slot<OpprettOppgave>()

    @BeforeEach
    internal fun setUp() {
        every { oppgaveService.opprettOppgave(any(), any()) } returns oppgaveId
    }

    @EnumSource(
        value = BehandlingStatus::class,
        names = ["OPPRETTET", "UTREDES", "SATT_PÅ_VENT"],
        mode = EnumSource.Mode.INCLUDE,
    )
    @ParameterizedTest
    internal fun `Skal opprette oppgave hvis behandlingen har status opprettet eller utredes`(behandlingStatus: BehandlingStatus) {
        val behandling = mockBehandling(behandlingStatus)

        opprettOppgaveForOpprettetBehandlingTask.doTask(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandling.id,
                    "",
                ),
            ),
        )

        verify(exactly = 1) { oppgaveService.opprettOppgave(any(), capture(opprettOppgaveSlot)) }
        if (behandlingStatus == BehandlingStatus.SATT_PÅ_VENT) {
            assertThat(opprettOppgaveSlot.captured.opprettIMappe).isEqualTo(OppgaveMappe.PÅ_VENT)
        } else {
            assertThat(opprettOppgaveSlot.captured.opprettIMappe).isEqualTo(OppgaveMappe.KLAR)
        }
    }

    @EnumSource(
        value = BehandlingStatus::class,
        names = ["OPPRETTET", "UTREDES", "SATT_PÅ_VENT"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    @ParameterizedTest
    internal fun `Skal ikke opprette oppgave hvis behandlingen ikke har status opprettet eller utredes`(
        behandlingStatus: BehandlingStatus,
    ) {
        val behandling = mockBehandling(behandlingStatus)

        opprettOppgaveForOpprettetBehandlingTask.doTask(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandling.id,
                    "",
                ),
            ),
        )

        verify(exactly = 0) { oppgaveService.opprettOppgave(any(), any()) }
    }

    @Test
    internal fun `Skal opprette oppgave med type BehandleKjøreliste for kjørelistebehandlinger`() {
        val behandling = mockBehandling(status = BehandlingStatus.OPPRETTET, type = BehandlingType.KJØRELISTE)

        opprettOppgaveForOpprettetBehandlingTask.doTask(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandling.id,
                    "",
                ),
            ),
        )

        verify(exactly = 1) { oppgaveService.opprettOppgave(any(), capture(opprettOppgaveSlot)) }
        assertThat(opprettOppgaveSlot.captured.oppgavetype).isEqualTo(Oppgavetype.BehandleKjøreliste)
    }

    @Test
    internal fun `Skal opprette oppgave med type BehandleSak for ikke-kjørelistebehandlinger`() {
        val behandling = mockBehandling(status = BehandlingStatus.OPPRETTET, type = BehandlingType.FØRSTEGANGSBEHANDLING)

        opprettOppgaveForOpprettetBehandlingTask.doTask(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandling.id,
                    "",
                ),
            ),
        )

        verify(exactly = 1) { oppgaveService.opprettOppgave(any(), capture(opprettOppgaveSlot)) }
        assertThat(opprettOppgaveSlot.captured.oppgavetype).isEqualTo(Oppgavetype.BehandleSak)
    }

    private fun mockBehandling(
        status: BehandlingStatus,
        type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    ): Saksbehandling {
        val behandling = saksbehandling(status = status, type = type)
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        return behandling
    }
}
