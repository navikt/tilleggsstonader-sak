package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil
import no.nav.tilleggsstonader.sak.util.oppgave
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AngreSendTilBeslutterServiceTest {

    private val oppgaveService = mockk<OppgaveService>()
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val totrinnskontrollService = mockk<TotrinnskontrollService>()

    val service = AngreSendTilBeslutterService(
        oppgaveService = oppgaveService,
        behandlingService = behandlingService,
        behandlingshistorikkService = mockk(relaxed = true),
        taskService = mockk(relaxed = true),
        totrinnskontrollService = totrinnskontrollService,
    )

    val behandling = saksbehandling(steg = StegType.BESLUTTE_VEDTAK, status = BehandlingStatus.FATTER_VEDTAK)
    val saksbehandler1 = "saksbehandler1"
    val saksbehandler2 = "saksbehandler2"
    val oppgave = oppgave(behandlingId = behandling.id)

    @BeforeEach
    fun setUp() {
        BrukerContextUtil.mockBrukerContext(saksbehandler1)
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        every { totrinnskontrollService.hentSaksbehandlerSomSendteTilBeslutter(behandling.id) } returns saksbehandler1
        every { totrinnskontrollService.hentBeslutter(behandling.id) } returns null
        every { oppgaveService.hentOppgaveSomIkkeErFerdigstilt(behandling.id, Oppgavetype.GodkjenneVedtak) } returns
            oppgave(behandlingId = behandling.id)
        every { oppgaveService.hentOppgave(oppgave.gsakOppgaveId) } returns Oppgave(id = 123, tilordnetRessurs = null)
    }

    @AfterEach
    fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    fun `skal oppdatere steg og status på behandling`() {
        service.angreSendTilBeslutter(behandling.id)

        verify(exactly = 1) {
            behandlingService.oppdaterStegPåBehandling(behandling.id, StegType.SEND_TIL_BESLUTTER)
            behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.UTREDES)
        }
    }

    @Nested
    inner class Validering {

        @Test
        fun `saksbehandler som angrer må være den som sendt til beslutter`() {
            BrukerContextUtil.mockBrukerContext(saksbehandler2)

            assertThat(
                catchThrowableOfType<ApiFeil> {
                    service.angreSendTilBeslutter(behandling.id)
                },
            ).hasMessageContaining("Kan kun angre send til beslutter dersom du er saksbehandler på vedtaket")
        }

        @Test
        fun `skal validere at steget ikke er før beslutte vedtak`() {
            every { behandlingService.hentSaksbehandling(behandling.id) } returns
                behandling.copy(steg = StegType.VILKÅR)

            assertThat(
                catchThrowableOfType<Feil> {
                    service.angreSendTilBeslutter(behandling.id)
                },
            ).hasMessageContaining("Kan ikke angre send til beslutter når behandling er i steg VILKÅR")
        }

        @Test
        fun `skal validere at steget ikke er etter beslutte vedtak`() {
            every { behandlingService.hentSaksbehandling(behandling.id) } returns
                behandling.copy(steg = StegType.VENTE_PÅ_STATUS_FRA_UTBETALING)

            assertThat(
                catchThrowableOfType<Feil> {
                    service.angreSendTilBeslutter(behandling.id)
                },
            ).hasMessageContaining("Kan ikke angre send til beslutter da vedtaket er godkjent av")
        }

        @Test
        fun `skal validere at steget er i fatter vedtak`() {
            BehandlingStatus.entries.filterNot { it == BehandlingStatus.FATTER_VEDTAK }.forEach {
                every { behandlingService.hentSaksbehandling(behandling.id) } returns
                    behandling.copy(status = it)
                assertThat(
                    catchThrowableOfType<Feil> {
                        service.angreSendTilBeslutter(behandling.id)
                    },
                ).hasMessageContaining("Kan ikke angre send til beslutter når behandlingen har status")
            }
        }

        @Test
        fun `skal kaste feil hvis saksbehandler ikke tilordnetRessurs på oppgaven`() {
            every { oppgaveService.hentOppgave(oppgave.gsakOppgaveId) } returns Oppgave(
                id = 123,
                tilordnetRessurs = saksbehandler2,
            )

            assertThat(
                catchThrowableOfType<ApiFeil> {
                    service.angreSendTilBeslutter(behandling.id)
                },
            ).hasMessageContaining("Kan ikke angre send til beslutter når oppgave er plukket av $saksbehandler2")
        }
    }
}
