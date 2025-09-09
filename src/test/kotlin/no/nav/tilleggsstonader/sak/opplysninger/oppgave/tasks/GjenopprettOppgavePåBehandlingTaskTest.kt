package no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.vent.SettPåVent
import no.nav.tilleggsstonader.sak.behandling.vent.SettPåVentRepository
import no.nav.tilleggsstonader.sak.behandling.vent.ÅrsakSettPåVent
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveRepository
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.Oppgavestatus
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.oppgave
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class GjenopprettOppgavePåBehandlingTaskTest {
    private val behandlingService: BehandlingService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val oppgaveRepository: OppgaveRepository = mockk()
    private val settPåVentRepository: SettPåVentRepository = mockk()
    private lateinit var taskStep: GjenopprettOppgavePåBehandlingTask

    @BeforeEach
    fun setUp() {
        taskStep =
            GjenopprettOppgavePåBehandlingTask(
                behandligService = behandlingService,
                oppgaveService = oppgaveService,
                oppgaveRepository = oppgaveRepository,
                settPåVentRepository = settPåVentRepository,
            )
    }

    @Test
    fun `skal ignorere tidligere oppgave og opprette ny når siste er feilregistrert og oppdatere settpåvent med oppgaveid`() {
        val behandling = behandling(status = BehandlingStatus.FATTER_VEDTAK)

        val sisteOppgave =
            oppgave(
                behandling = behandling,
                status = Oppgavestatus.FEILREGISTRERT,
            )

        val settPåVent =
            SettPåVent(
                id = UUID.randomUUID(),
                behandlingId = behandling.id,
                oppgaveId = 0L,
                årsaker = ÅrsakSettPåVent.entries.toList(),
                kommentar = "",
                aktiv = true,
            )

        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { oppgaveService.finnSisteOppgaveForBehandling(behandling.id) } returns sisteOppgave
        every { settPåVentRepository.findByBehandlingIdAndAktivIsTrue(behandling.id) } returns settPåVent
        every { settPåVentRepository.update(any()) } answers { firstArg() }

        val opprettOppgaveSlot = slot<OpprettOppgave>()
        every { oppgaveService.opprettOppgave(behandling.id, capture(opprettOppgaveSlot)) } returns 1L

        val task: Task = GjenopprettOppgavePåBehandlingTask.opprettTask(behandling.id)

        taskStep.doTask(task)

        verify(exactly = 0) { oppgaveRepository.update(any()) }
        verify(exactly = 1) { oppgaveService.opprettOppgave(behandling.id, any()) }
        verify(exactly = 1) { settPåVentRepository.update(settPåVent.copy(oppgaveId = 1L)) }

        val ny = opprettOppgaveSlot.captured
        assertThat(ny.beskrivelse).contains("feilregistrert")
        assertThat(ny.oppgavetype).isEqualTo(Oppgavetype.GodkjenneVedtak)
    }

    @Test
    fun `skal ikke ignorere tidligere oppgave men opprette ny når siste er aapen`() {
        val behandling = behandling()

        val sisteOppgave =
            oppgave(
                behandling = behandling,
                status = Oppgavestatus.ÅPEN,
            )

        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { oppgaveService.finnSisteOppgaveForBehandling(behandling.id) } returns sisteOppgave

        val opprettOppgaveSlot = slot<OpprettOppgave>()
        every { oppgaveService.opprettOppgave(behandling.id, capture(opprettOppgaveSlot)) } returns 1L
        every { oppgaveRepository.update(any()) } returns sisteOppgave.copy(status = Oppgavestatus.IGNORERT)
        every { settPåVentRepository.findByBehandlingIdAndAktivIsTrue(any()) } returns null

        val task: Task = GjenopprettOppgavePåBehandlingTask.opprettTask(behandling.id)

        taskStep.doTask(task)

        verify(exactly = 1) {
            oppgaveRepository.update(
                withArg {
                    assertThat(it.status).isEqualTo(Oppgavestatus.IGNORERT)
                    assertThat(it.id).isEqualTo(sisteOppgave.id)
                },
            )
        }
        verify(exactly = 1) { oppgaveService.opprettOppgave(behandling.id, any()) }

        val ny = opprettOppgaveSlot.captured
        assertThat(ny.beskrivelse).contains("flyttet eller endret")
        assertThat(ny.oppgavetype).isEqualTo(Oppgavetype.BehandleSak)
    }
}
