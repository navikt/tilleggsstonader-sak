package no.nav.tilleggsstonader.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollRepository
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Årsaker
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.SendTilBeslutterRequest
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.TotrinnkontrollStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.ÅrsakUnderkjent
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class TotrinnskontrollServiceTest {
    private val behandlingshistorikkService = mockk<BehandlingshistorikkService>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val tilgangService = mockk<TilgangService>()
    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val taskService = mockk<TaskService>(relaxed = true)
    private val totrinnskontrollRepository = mockk<TotrinnskontrollRepository>()

    private val totrinnskontrollService =
        TotrinnskontrollService(
            behandlingshistorikkService = behandlingshistorikkService,
            behandlingService = behandlingService,
            tilgangService = tilgangService,
            taskService = taskService,
            totrinnskontrollRepository = totrinnskontrollRepository,
        )

    val saksbehandler = "Behandler"
    val beslutter = "beslutter"

    @BeforeEach
    internal fun setUp() {
        every { tilgangService.harTilgangTilRolle(any()) } returns true
        every { tilkjentYtelseRepository.findByBehandlingId(any()) } returns null
        every {
            totrinnskontrollRepository.insert(any())
        } answers { firstArg() }
        every {
            totrinnskontrollRepository.update(any())
        } answers { firstArg() }
        every {
            totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any())
        } returns totrinnskontroll(opprettetAv = saksbehandler, status = TotrinnInternStatus.KAN_FATTE_VEDTAK)
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.UTREDES)
        BrukerContextUtil.mockBrukerContext(saksbehandler)
    }

    @AfterEach
    fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal returnere saksbehandler som sendte behandling til besluttning`() {
        val response =
            testWithBrukerContext(beslutter) {
                totrinnskontrollService
                    .lagreTotrinnskontrollOgReturnerSaksbehandler(
                        saksbehandling(status = BehandlingStatus.UTREDES),
                        BeslutteVedtakDto(true, ""),
                    )
            }
        assertThat(response).isEqualTo(saksbehandler)
    }

    @Test
    internal fun `skal kaste feil når beslutter er samme som saksbehandler`() {
        assertThatThrownBy {
            totrinnskontrollService.lagreTotrinnskontrollOgReturnerSaksbehandler(
                saksbehandling(status = BehandlingStatus.UTREDES),
                BeslutteVedtakDto(true, ""),
            )
        }.hasMessageContaining("Beslutter er samme som saksbehandler, kan ikke utføre totrinnskontroll")
    }

    @Test
    internal fun `totrinnskontroll eksisterer og har ikkje underkjent eller angret som status`() {
        assertThatThrownBy {
            totrinnskontrollService.sendtilBeslutter(saksbehandling(status = BehandlingStatus.UTREDES), SendTilBeslutterRequest())
        }.hasMessage("Kan ikke sende til beslutter da det eksisterer en totrinnskontroll med status=KAN_FATTE_VEDTAK")
    }

    @Test
    internal fun `totrinnskontroll settes til angret fra saksbehandler`() {
        assertDoesNotThrow("Should not throw an exception") {
            totrinnskontrollService.angreSendTilBeslutter(BehandlingId.random())
        }
    }

    @Test
    internal fun `totrinnskontroll skal feile på angre om den ikke har status kan fatte vedtak`() {
        every {
            totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any())
        } returns totrinnskontroll(opprettetAv = saksbehandler, TotrinnInternStatus.UNDERKJENT)
        assertThatThrownBy {
            totrinnskontrollService.angreSendTilBeslutter(BehandlingId.random())
        }.hasMessageContaining("Kan ikke angre når status=${TotrinnInternStatus.UNDERKJENT}")
    }

    @Test
    internal fun ` totrinnskontroll eksisterer men har underkjent eller angret som status`() {
        val behandling = saksbehandling(status = BehandlingStatus.UTREDES)
        every {
            totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any())
        } returns totrinnskontroll(opprettetAv = saksbehandler, TotrinnInternStatus.UNDERKJENT)

        assertDoesNotThrow {
            totrinnskontrollService.sendtilBeslutter(behandling, SendTilBeslutterRequest())
        }
    }

    @Test
    internal fun ` totrinnskontroll eksisterer ikke før opprettelse`() {
        val saksbehandling = saksbehandling(status = BehandlingStatus.UTREDES)
        every { totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any()) } returns null

        assertDoesNotThrow {
            totrinnskontrollService.sendtilBeslutter(saksbehandling, SendTilBeslutterRequest())
        }
    }

    @Test
    internal fun `skal utlede saksbehandler som sendte behandling til besluttning`() {
        val response = totrinnskontrollService.hentSaksbehandlerSomSendteTilBeslutter(BehandlingId.random())

        assertThat(response).isEqualTo(saksbehandler)
    }

    @Test
    internal fun `skal returnere UAKTUELT når behandlingen FERDIGSTILT`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.FERDIGSTILT)

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(BEHANDLING_ID)

        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.UAKTUELT)
        assertThat(totrinnskontroll.totrinnskontroll).isNull()
    }

    @Test
    internal fun `skal returnere UAKTUELT når behandlingen IVERKSETTER_VEDTAK`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.IVERKSETTER_VEDTAK)

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(BEHANDLING_ID)

        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.UAKTUELT)
        assertThat(totrinnskontroll.totrinnskontroll).isNull()
    }

    @Test
    internal fun `skal returnere UAKTUELT når behandlingen UTREDES og ikke har noen totrinnshistorikk`() {
        every { totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any()) } returns null
        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(BEHANDLING_ID)

        assertThat(totrinnskontroll.totrinnskontroll).isNull()
    }

    @Test
    internal fun `skal returnere TOTRINNSKONTROLL_UNDERKJENT når behandlingen UTREDES og vedtak er underkjent`() {
        every { totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any()) } returns
            totrinnskontrollMedbeslutterAArsakogBegrunnelse(
                opprettetAv = saksbehandler,
                beslutter = beslutter,
            )

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(BEHANDLING_ID)

        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.TOTRINNSKONTROLL_UNDERKJENT)
        assertThat(totrinnskontroll.totrinnskontroll?.begrunnelse).isEqualTo("begrunnelse underkjent")
    }

    /*
     * skal returnere KAN_FATTE_VEDTAK når behandlingen FATTER_VEDTAK og saksbehandler er utreder og ikke er den som
     * sendte behandlingen til fatte vedtak
     */
    @Test
    internal fun `skal returnere KAN_FATTE_VEDTAK i gitt situasjon`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.FATTER_VEDTAK)
        every { totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any()) } returns
            totrinnskontroll(
                opprettetAv = "Annen saksbehandler",
                status = TotrinnInternStatus.GODKJENT,
            )

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(BEHANDLING_ID)

        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.KAN_FATTE_VEDTAK)
    }

    /*
     * skal returnere IKKE_AUTORISERT når behandlingen FATTER_VEDTAK og saksbehandler er utreder, men er den som
     * sendte behandlingen til fatte vedtak
     */
    @Test
    internal fun `skal returnere IKKE_AUTORISERT i gitt situasjon`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.FATTER_VEDTAK)
        every { totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any()) } returns
            totrinnskontroll(opprettetAv = beslutter)

        val totrinnskontroll =
            testWithBrukerContext(beslutter) {
                totrinnskontrollService.hentTotrinnskontrollStatus(BEHANDLING_ID)
            }

        assertThat(totrinnskontroll.totrinnskontroll).isNotNull
        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.IKKE_AUTORISERT)
    }

    @Test
    internal fun `skal returnere IKKE_AUTORISERT når behandlingen FATTER_VEDTAK og saksbehandler ikke er utreder`() {
        every { tilgangService.harTilgangTilRolle(any()) } returns false
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.FATTER_VEDTAK)
        every { totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any()) } returns
            totrinnskontroll(
                status = TotrinnInternStatus.KAN_FATTE_VEDTAK,
                opprettetAv = "Annen saksbehandler",
            )

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(BEHANDLING_ID)

        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.IKKE_AUTORISERT)
        assertThat(totrinnskontroll.totrinnskontroll).isNotNull
        verify(exactly = 1) { tilgangService.harTilgangTilRolle(any()) }
    }

    @Test
    internal fun `skal kaste feil når behandlingstatus er UTREDES og utfall er GODKJENT`() {
        every { totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any()) } returns
            totrinnskontroll(
                opprettetAv = "Annen saksbehandler",
                status = TotrinnInternStatus.GODKJENT,
            )

        Assertions
            .assertThat(
                Assertions.catchThrowable {
                    totrinnskontrollService.hentTotrinnskontrollStatus(
                        BEHANDLING_ID,
                    )
                },
            ).hasMessageContaining("Skal ikke kunne være annen status enn UNDERKJENT")
    }

    @Test
    internal fun `skal returnere begrunnelse og årsaker underkjent når vedtak er underkjent`() {
        every { totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any()) } returns
            totrinnskontrollMedbeslutterAArsakogBegrunnelse(
                opprettetAv = "Noe",
                beslutter = "Noen annen",
            )

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(BEHANDLING_ID)

        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.TOTRINNSKONTROLL_UNDERKJENT)
        assertThat(totrinnskontroll.totrinnskontroll?.begrunnelse).isEqualTo("begrunnelse underkjent")
        assertThat(totrinnskontroll.totrinnskontroll?.årsakerUnderkjent).containsExactlyInAnyOrder(
            ÅrsakUnderkjent.VEDTAKSBREV,
            ÅrsakUnderkjent.VEDTAK_OG_BEREGNING,
        )
    }

    @Test
    internal fun `skal kunne underkjenne en totrinnskontroll`() {
        val oppdaterSlot = slot<Totrinnskontroll>()
        every {
            totrinnskontrollRepository.update(capture(oppdaterSlot))
        } answers { firstArg() }

        testWithBrukerContext(beslutter) {
            totrinnskontrollService.lagreTotrinnskontrollOgReturnerSaksbehandler(
                saksbehandling(status = BehandlingStatus.UTREDES),
                BeslutteVedtakDto(false, "manglende", årsakerUnderkjent = listOf(ÅrsakUnderkjent.VEDTAKSBREV)),
            )
        }
        assertThat(oppdaterSlot.captured.årsakerUnderkjent?.årsaker!!).containsExactly(ÅrsakUnderkjent.VEDTAKSBREV)
    }

    @Nested
    inner class KommentarTilBeslutter {
        @Test
        internal fun `skal kunne legge ved kommentar om behandling tidlere er underkjent`() {
            val behandling = saksbehandling(status = BehandlingStatus.UTREDES)
            every {
                totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any())
            } returns totrinnskontroll(opprettetAv = saksbehandler, TotrinnInternStatus.UNDERKJENT)

            val oppdaterSlot = slot<Totrinnskontroll>()
            every {
                totrinnskontrollRepository.insert(capture(oppdaterSlot))
            } answers { firstArg() }

            assertDoesNotThrow {
                totrinnskontrollService.sendtilBeslutter(behandling, SendTilBeslutterRequest("kommentar"))
            }

            assertThat(oppdaterSlot.captured.status).isEqualTo(TotrinnInternStatus.KAN_FATTE_VEDTAK)
            assertThat(oppdaterSlot.captured.begrunnelse).isNotNull()
        }

        @Test
        internal fun `skal ikke kunne legge ved kommentar om det er første gang behandling sendes til beslutter`() {
            val behandling = saksbehandling(status = BehandlingStatus.UTREDES)
            every {
                totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any())
            } returns null

            assertThatThrownBy {
                totrinnskontrollService.sendtilBeslutter(behandling, SendTilBeslutterRequest("kommentar"))
            }.hasMessageContaining("Kan ikke legge ved kommentar til beslutter dersom behandlingen ikke er tidligere underkjent")
        }
    }

    private fun totrinnskontroll(
        opprettetAv: String,
        status: TotrinnInternStatus = TotrinnInternStatus.KAN_FATTE_VEDTAK,
    ) = Totrinnskontroll(
        behandlingId = BehandlingId.random(),
        sporbar = Sporbar(opprettetAv),
        status = status,
        saksbehandler = opprettetAv,
    )

    private fun totrinnskontrollMedbeslutterAArsakogBegrunnelse(
        opprettetAv: String,
        beslutter: String,
    ) = Totrinnskontroll(
        behandlingId = BehandlingId.random(),
        sporbar = Sporbar(opprettetAv),
        status = TotrinnInternStatus.UNDERKJENT,
        saksbehandler = opprettetAv,
        beslutter = beslutter,
        årsakerUnderkjent =
            Årsaker(
                listOf(
                    ÅrsakUnderkjent.VEDTAKSBREV,
                    ÅrsakUnderkjent.VEDTAK_OG_BEREGNING,
                ),
            ),
        begrunnelse = "begrunnelse underkjent",
    )

    private fun behandling(status: BehandlingStatus) = behandling(fagsak, status, steg = StegType.BESLUTTE_VEDTAK)

    companion object {
        private val BEHANDLING_ID = BehandlingId.random()
        private val fagsak = fagsak()
    }
}
