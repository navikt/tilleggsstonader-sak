package no.nav.tilleggsstonader.sak.tilordnetSaksbehandler

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Saksbehandler
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveRepository
import no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler.TilordnetSaksbehandlerClient
import no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler.TilordnetSaksbehandlerService
import no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler.domain.TilordnetSaksbehandlerPåOppgave
import no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler.dto.TilordnetSaksbehandlerDto
import no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler.dto.tilDto
import no.nav.tilleggsstonader.sak.util.BehandlingOppsettUtil.iverksattFørstegangsbehandling
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class TilordnetSaksbehandlerServiceTest {
    private val oppgaveRepository = mockk<OppgaveRepository>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val saksbehandlerClient = mockk<TilordnetSaksbehandlerClient>()
    private lateinit var service: TilordnetSaksbehandlerService

    private val behandlingId = BehandlingId(UUID.randomUUID())
    private val saksbehandlerInfo = Saksbehandler(UUID.randomUUID(), "Z123456", "Test", "Testesen", "TSO")

    @BeforeEach
    fun setUp() {
        service =
            TilordnetSaksbehandlerService(
                oppgaveRepository,
                behandlingRepository,
                saksbehandlerClient,
            )
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
                tilordnetSaksbehandler = saksbehandlerInfo.navIdent,
            )

        every { behandlingRepository.findByIdOrThrow(behandlingId) } returns behandling
        every {
            oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(any(), any())
        } returns oppgave
        every { saksbehandlerClient.hentSaksbehandlerInfo(saksbehandlerInfo.navIdent) } returns saksbehandlerInfo

        testWithBrukerContext(saksbehandlerInfo.navIdent) {
            val resultat = service.finnTilordnetSaksbehandler(behandlingId)

            assertThat(resultat.tilordnetSaksbehandlerPåOppgave).isEqualTo(TilordnetSaksbehandlerPåOppgave.INNLOGGET_SAKSBEHANDLER)
            assertThat(resultat.fornavn).isEqualTo("Test")
            assertThat(resultat.etternavn).isEqualTo("Testesen")
        }
    }

    @Test
    fun `skal returnere OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER hvis oppgave mangler og behandling er i visse steg`() {
        val behandling =
            iverksattFørstegangsbehandling.copy(steg = StegType.INNGANGSVILKÅR, status = BehandlingStatus.UTREDES)

        every { behandlingRepository.findByIdOrThrow(behandlingId) } returns behandling
        every {
            oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(any(), any())
        } returns null
        every { saksbehandlerClient.hentSaksbehandlerInfo(saksbehandlerInfo.navIdent) } returns saksbehandlerInfo

        testWithBrukerContext(saksbehandlerInfo.navIdent) {
            val resultat = service.finnTilordnetSaksbehandler(behandlingId)

            assertThat(
                resultat.tilordnetSaksbehandlerPåOppgave,
            ).isEqualTo(TilordnetSaksbehandlerPåOppgave.OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER)
            assertThat(resultat.fornavn).isEqualTo("Test")
            assertThat(resultat.etternavn).isEqualTo("Testesen")
        }
    }

    @Test
    fun `skal returnere OPPGAVE_FINNES_IKKE hvis oppgave er null og behandling ikke er i visse steg`() {
        val behandling =
            iverksattFørstegangsbehandling.copy(steg = StegType.SEND_TIL_BESLUTTER, status = BehandlingStatus.UTREDES)

        every { behandlingRepository.findByIdOrThrow(behandlingId) } returns behandling
        every {
            oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(any(), any())
        } returns null
        every { saksbehandlerClient.hentSaksbehandlerInfo(saksbehandlerInfo.navIdent) } returns saksbehandlerInfo

        testWithBrukerContext(saksbehandlerInfo.navIdent) {
            val resultat = service.finnTilordnetSaksbehandler(behandlingId)

            assertThat(resultat.tilordnetSaksbehandlerPåOppgave).isEqualTo(TilordnetSaksbehandlerPåOppgave.OPPGAVE_FINNES_IKKE)
            assertThat(resultat.fornavn).isEqualTo(null)
            assertThat(resultat.etternavn).isEqualTo(null)
        }
    }

    @Test
    fun `skal returnere IKKE_SATT hvis tilordnetSaksbehandler på oppgaven er null`() {
        val behandling =
            iverksattFørstegangsbehandling.copy(steg = StegType.INNGANGSVILKÅR, status = BehandlingStatus.UTREDES)
        val oppgave =
            OppgaveDomain(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                gsakOppgaveId = 1L,
                type = Oppgavetype.BehandleSak,
                erFerdigstilt = false,
                tilordnetSaksbehandler = null,
            )

        every { behandlingRepository.findByIdOrThrow(behandlingId) } returns behandling
        every {
            oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(any(), any())
        } returns oppgave
        every { saksbehandlerClient.hentSaksbehandlerInfo(saksbehandlerInfo.navIdent) } returns saksbehandlerInfo

        testWithBrukerContext(saksbehandlerInfo.navIdent) {
            val resultat = service.finnTilordnetSaksbehandler(behandlingId)

            assertThat(resultat.tilordnetSaksbehandlerPåOppgave).isEqualTo(TilordnetSaksbehandlerPåOppgave.IKKE_SATT)
            assertThat(resultat.fornavn).isEqualTo(null)
            assertThat(resultat.etternavn).isEqualTo(null)
        }
    }

    @Test
    fun `skal returnere null og OPPGAVE_FINNES_IKKE hvis behandlingen er ferdigstilt`() {
        val ferdigstiltBehandling =
            iverksattFørstegangsbehandling.copy(
                steg = StegType.FERDIGSTILLE_BEHANDLING,
                status = BehandlingStatus.FERDIGSTILT,
            )

        every { behandlingRepository.findByIdOrThrow(behandlingId) } returns ferdigstiltBehandling

        every {
            oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
                any(),
                any(),
            )
        } returns null

        val resultat = service.finnTilordnetSaksbehandler(behandlingId).tilDto()

        assertThat(resultat).isEqualTo(
            TilordnetSaksbehandlerDto(
                navIdent = null,
                fornavn = null,
                etternavn = null,
                tilordnetSaksbehandlerPåOppgave = TilordnetSaksbehandlerPåOppgave.OPPGAVE_FINNES_IKKE,
            ),
        )
    }

    @Test
    fun `skal returnere ANNEN_SAKSBEHANDLER når innlogget ikke er tilordnet oppgave`() {
        val innloggetSaksbehandlerSomIkkeEierBehandling =
            Saksbehandler(UUID.randomUUID(), "Z223344", "Test", "Mockk", "TSO")

        val behandling =
            iverksattFørstegangsbehandling.copy(steg = StegType.INNGANGSVILKÅR, status = BehandlingStatus.UTREDES)
        val oppgave =
            OppgaveDomain(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                gsakOppgaveId = 1L,
                type = Oppgavetype.BehandleSak,
                erFerdigstilt = false,
                tilordnetSaksbehandler = saksbehandlerInfo.navIdent,
            )

        every { saksbehandlerClient.hentSaksbehandlerInfo(innloggetSaksbehandlerSomIkkeEierBehandling.navIdent) } returns
            innloggetSaksbehandlerSomIkkeEierBehandling

        every { saksbehandlerClient.hentSaksbehandlerInfo(saksbehandlerInfo.navIdent) } returns saksbehandlerInfo

        every { behandlingRepository.findByIdOrThrow(behandlingId) } returns behandling
        every { oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(any(), any()) } returns oppgave

        testWithBrukerContext(innloggetSaksbehandlerSomIkkeEierBehandling.navIdent) {
            val resultat = service.finnTilordnetSaksbehandler(behandlingId)

            assertThat(oppgave.tilordnetSaksbehandler).isNotEqualTo(innloggetSaksbehandlerSomIkkeEierBehandling.navIdent)
            assertThat(resultat.tilordnetSaksbehandlerPåOppgave).isEqualTo(TilordnetSaksbehandlerPåOppgave.ANNEN_SAKSBEHANDLER)
            assertThat(resultat.fornavn).isEqualTo("Test")
            assertThat(resultat.etternavn).isEqualTo("Testesen")
        }
    }
}
