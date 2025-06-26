package no.nav.tilleggsstonader.sak.saksbehandler

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import no.nav.tilleggsstonader.kontrakter.felles.Saksbehandler
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveRepository
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.SaksbehandlerDto
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.SaksbehandlerRolle
import no.nav.tilleggsstonader.sak.opplysninger.saksbehandler.SaksbehandlerClient
import no.nav.tilleggsstonader.sak.opplysninger.saksbehandler.SaksbehandlerService
import no.nav.tilleggsstonader.sak.util.BehandlingOppsettUtil.iverksattFørstegangsbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID

internal class SaksbehandlerServiceTest {
    private val oppgaveRepository = mockk<OppgaveRepository>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val saksbehandlerClient = mockk<SaksbehandlerClient>()
    private lateinit var service: SaksbehandlerService

    private val behandlingId = BehandlingId(UUID.randomUUID())
    private val innloggetSaksbehandler = "Z123456"

    @BeforeEach
    fun setUp() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler() } returns innloggetSaksbehandler
        service = SaksbehandlerService(oppgaveRepository, behandlingRepository, saksbehandlerClient)
    }

    @Test
    fun `skal returnere INNLOGGET_SAKSBEHANDLER hvis oppgaven er tilordnet innlogget saksbehandler`() {
        val behandling =
            iverksattFørstegangsbehandling.copy(steg = StegType.INNGANGSVILKÅR, status = BehandlingStatus.UTREDES)
        val oppgave =
            OppgaveDomain(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                gsakOppgaveId = 1L,
                type = Oppgavetype.BehandleSak,
                erFerdigstilt = false,
                tilordnetSaksbehandler = innloggetSaksbehandler,
            )
        val saksbehandlerInfo = Saksbehandler(UUID.randomUUID(), "Z123456", "Test", "Testesen", "TSO")

        every { behandlingRepository.findByIdOrThrow(behandlingId) } returns behandling
        every {
            oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(any(), any())
        } returns oppgave
        every { saksbehandlerClient.hentSaksbehandlerInfo(innloggetSaksbehandler) } returns saksbehandlerInfo

        val resultat = service.finnSaksbehandler(behandlingId)

        assertThat(resultat?.rolle).isEqualTo(SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER)
        assertThat(resultat?.fornavn).isEqualTo("Test")
        assertThat(resultat?.etternavn).isEqualTo("Testesen")
    }

    @Test
    fun `skal returnere OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER hvis oppgave mangler og behandling er i visse steg`() {
        val behandling =
            iverksattFørstegangsbehandling.copy(steg = StegType.INNGANGSVILKÅR, status = BehandlingStatus.UTREDES)
        val saksbehandlerInfo = Saksbehandler(UUID.randomUUID(), "Z123456", "Test", "Testesen", "TSO")

        every { behandlingRepository.findByIdOrThrow(behandlingId) } returns behandling
        every {
            oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(any(), any())
        } returns null
        every { saksbehandlerClient.hentSaksbehandlerInfo(innloggetSaksbehandler) } returns saksbehandlerInfo

        val resultat = service.finnSaksbehandler(behandlingId)

        assertThat(resultat?.rolle).isEqualTo(SaksbehandlerRolle.OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER)
        assertThat(resultat?.fornavn).isEqualTo("Test")
        assertThat(resultat?.etternavn).isEqualTo("Testesen")
    }

    @Test
    fun `skal returnere OPPGAVE_FINNES_IKKE hvis oppgaven er null`() {
        val behandling =
            iverksattFørstegangsbehandling.copy(
                steg = StegType.FERDIGSTILLE_BEHANDLING, // Et steg der det ikke skal finnes oppgave
                status = BehandlingStatus.UTREDES,
            )

        every { behandlingRepository.findByIdOrNull(behandlingId) } returns behandling
        every {
            oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(behandling.id, any())
        } returns null

        val resultat = service.finnSaksbehandler(behandlingId)

        assertThat(resultat?.rolle).isEqualTo(SaksbehandlerRolle.OPPGAVE_FINNES_IKKE)
    }

    @Test
    fun `skal returnere null hvis behandlingen er ferdigstilt`() {
        val ferdigstiltBehandling =
            iverksattFørstegangsbehandling.copy(
                steg = StegType.FERDIGSTILLE_BEHANDLING,
                status = BehandlingStatus.FERDIGSTILT,
            )

        every { behandlingRepository.findByIdOrThrow(behandlingId) } returns ferdigstiltBehandling

        every { oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(any(), emptySet()) } returns null

        val resultat = service.finnSaksbehandler(behandlingId)

        assertThat(resultat).isEqualTo(
            SaksbehandlerDto(
                fornavn = null,
                etternavn = null,
                rolle = SaksbehandlerRolle.OPPGAVE_FINNES_IKKE,
            ),
        )
    }

    @Test
    fun `skal returnere ANNEN_SAKSBEHANDLER når innlogget ikke er tilordnet oppgave`() {
        val innloggetSaksbehandlerSomIkkeEierBehandling = "Z654321"
        val behandling =
            iverksattFørstegangsbehandling.copy(steg = StegType.INNGANGSVILKÅR, status = BehandlingStatus.UTREDES)
        val oppgave =
            OppgaveDomain(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                gsakOppgaveId = 1L,
                type = Oppgavetype.BehandleSak,
                erFerdigstilt = false,
                tilordnetSaksbehandler = innloggetSaksbehandlerSomIkkeEierBehandling,
            )

        val saksbehandlerInfo =
            Saksbehandler(UUID.randomUUID(), innloggetSaksbehandlerSomIkkeEierBehandling, "Test", "Testesen", "TSO")

        every { behandlingRepository.findByIdOrThrow(behandlingId) } returns behandling
        every { oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(any(), any()) } returns oppgave
        every { saksbehandlerClient.hentSaksbehandlerInfo(innloggetSaksbehandlerSomIkkeEierBehandling) } returns saksbehandlerInfo

        val resultat = service.finnSaksbehandler(behandlingId)

        assertThat(oppgave.tilordnetSaksbehandler).isNotEqualTo(SikkerhetContext.hentSaksbehandler())
        assertThat(resultat?.rolle).isEqualTo(SaksbehandlerRolle.ANNEN_SAKSBEHANDLER)
        assertThat(resultat?.fornavn).isEqualTo("Test")
        assertThat(resultat?.etternavn).isEqualTo("Testesen")
    }
}
