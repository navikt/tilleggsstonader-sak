package no.nav.tilleggsstonader.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollRepository
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Årsaker
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.TotrinnkontrollStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.ÅrsakUnderkjent
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.*

internal class TotrinnskontrollServiceTest {

    private val behandlingshistorikkService = mockk<BehandlingshistorikkService>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val tilgangService = mockk<TilgangService>()
    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val totrinnskontrollRepository = mockk<TotrinnskontrollRepository>()
    private val totrinnskontrollService =
        TotrinnskontrollService(behandlingshistorikkService, behandlingService, tilgangService, totrinnskontrollRepository)

    @BeforeEach
    internal fun setUp() {
        val opprettetAvStandard = "Behandler"
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
        } returns totrinnskontroll(opprettetAv = opprettetAvStandard, status = TotrinnInternStatus.KAN_FATTE_VEDTAK)
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.UTREDES)
    }

    @Test
    internal fun `skal returnere saksbehandler som sendte behandling til besluttning`() {
        val opprettetAv = "Behandler"
        val response = totrinnskontrollService
            .lagreTotrinnskontrollOgReturnerSaksbehandler(
                saksbehandling(status = BehandlingStatus.UTREDES),
                BeslutteVedtakDto(true, ""),
            )
        assertThat(response).isEqualTo(opprettetAv)
    }

    @Test
    internal fun `skal kaste feil når beslutter er samme som saksbehandler`() {
        val opprettetAv = "Z991234"
        val beslutter = "Z991234"

        every {
            totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any())
        } returns totrinnskontrollMedbeslutter(opprettetAv = opprettetAv, beslutter = beslutter)
        assertThatThrownBy {
            totrinnskontrollService.lagreTotrinnskontrollOgReturnerSaksbehandler(
                saksbehandling(status = BehandlingStatus.UTREDES),
                BeslutteVedtakDto(true, ""),
            )
        }
    }

    @Test
    internal fun `totrinnskontroll eksisterer og har ikkje underkjent eller angret som status`() {
        assertThatThrownBy {
            totrinnskontrollService
                .sendtilBeslutter(
                    saksbehandling(status = BehandlingStatus.UTREDES),
                )
        }
    }

    @Test
    internal fun `totrinnskontroll settes til angret fra saksbehandler`() {
        assertDoesNotThrow("Should not throw an exception") {
            totrinnskontrollService.angreSendTilBeslutter(UUID.randomUUID())
        }
    }

    @Test
    internal fun `totrinnskontroll skal feile på angre om den ikke har status kan fatte vedtak`() {
        val opprettetAv = "Behandler"
        every {
            totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any())
        } returns totrinnskontroll(opprettetAv = opprettetAv, TotrinnInternStatus.UNDERKJENT)
        val result = assertThatThrownBy {
            totrinnskontrollService.angreSendTilBeslutter(UUID.randomUUID())
        }

        assertThat(result).isNotNull
    }

    @Test
    internal fun ` totrinnskontroll eksisterer men har underkjent eller angret som status`() {
        val opprettetAv = "Behandler"
        every {
            totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any())
        } returns totrinnskontroll(opprettetAv = opprettetAv, TotrinnInternStatus.UNDERKJENT)
        assertDoesNotThrow {
            totrinnskontrollService
                .sendtilBeslutter(
                    saksbehandling(status = BehandlingStatus.UTREDES),
                )
        }
    }

    @Test
    internal fun ` totrinnskontroll eksisterer ikke før opprettelse`() {
        every { totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any()) } returns null
        assertDoesNotThrow {
            totrinnskontrollService
                .sendtilBeslutter(
                    saksbehandling(status = BehandlingStatus.UTREDES),
                )
        }
    }

    @Test
    internal fun `skal utlede saksbehandler som sendte behandling til besluttning`() {
        val opprettetAv = "Behandler"
        every {
            totrinnskontrollRepository.findTopByBehandlingIdAndStatusOrderBySporbarEndretEndretTidDesc(any(), status = TotrinnInternStatus.GODKJENT)
        } returns
            totrinnskontroll(opprettetAv, TotrinnInternStatus.GODKJENT)

        val response = totrinnskontrollService
            .hentSaksbehandlerSomSendteTilBeslutter(UUID.randomUUID())

        assertThat(response).isEqualTo(opprettetAv)
    }

    @Test
    internal fun `skal returnere UAKTUELT når behandlingen FERDIGSTILT`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.FERDIGSTILT)

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)

        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.UAKTUELT)
        assertThat(totrinnskontroll.totrinnskontroll).isNull()
    }

    @Test
    internal fun `skal returnere UAKTUELT når behandlingen IVERKSETTER_VEDTAK`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.IVERKSETTER_VEDTAK)

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)

        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.UAKTUELT)
        assertThat(totrinnskontroll.totrinnskontroll).isNull()
    }

    @Test
    internal fun `skal returnere UAKTUELT når behandlingen UTREDES og ikke har noen totrinnshistorikk`() {
        every { totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any()) } returns null
        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)

        assertThat(totrinnskontroll.totrinnskontroll).isNull()
    }

    @Test
    internal fun `skal returnere TOTRINNSKONTROLL_UNDERKJENT når behandlingen UTREDES og vedtak er underkjent`() {
        every { totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any()) } returns
            totrinnskontrollMedbeslutterAArsakogBegrunnelse(
                opprettetAv = "Noe",
                beslutter = "noen to",
            )

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)

        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.TOTRINNSKONTROLL_UNDERKJENT)
        assertThat(totrinnskontroll.totrinnskontroll?.begrunnelse).isEqualTo("begrunnelse underkjent")
    }

    @Test
    internal fun `skal returnere KAN_FATTE_VEDTAK når behandlingen FATTER_VEDTAK og saksbehandler er utreder og ikke er den som sendte behandlingen til fatte vedtak`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.FATTER_VEDTAK)
        every { totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any()) } returns
            totrinnskontroll(
                opprettetAv = "Annen saksbehandler",
                status = TotrinnInternStatus.GODKJENT,
            )

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)

        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.KAN_FATTE_VEDTAK)
        assertThat(totrinnskontroll.totrinnskontroll).isNull()
    }

    @Test
    internal fun `skal returnere IKKE_AUTORISERT når behandlingen FATTER_VEDTAK og saksbehandler er utreder, men er den som sendte behandlingen til fatte vedtak`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.FATTER_VEDTAK)
        every { totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any()) } returns
            totrinnskontrollMedbeslutter(
                opprettetAv = "Beslutter",
                beslutter = "Beslutter",
            )

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)

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

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)

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

        Assertions.assertThat(Assertions.catchThrowable { totrinnskontrollService.hentTotrinnskontrollStatus(ID) })
            .hasMessageContaining("Skal ikke kunne være annen status enn UNDERKJENT")
    }

    @Test
    internal fun `skal returnere begrunnelse og årsaker underkjent når vedtak er underkjent`() {
        every { totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(any()) } returns
            totrinnskontrollMedbeslutterAArsakogBegrunnelse(
                opprettetAv = "Noe",
                beslutter = "Noen annen",
            )

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)

        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.TOTRINNSKONTROLL_UNDERKJENT)
        assertThat(totrinnskontroll.totrinnskontroll?.begrunnelse).isEqualTo("begrunnelse underkjent")
        assertThat(totrinnskontroll.totrinnskontroll?.årsakerUnderkjent).containsExactlyInAnyOrder(ÅrsakUnderkjent.INNGANGSVILKÅR_FORUTGÅENDE_MEDLEMSKAP_OPPHOLD, ÅrsakUnderkjent.VEDTAK_OG_BEREGNING)
    }

    @Test
    internal fun `skal kunne underkjenne en totrinnskontroll`() {
        val oppdaterSlot = slot<Totrinnskontroll>()
        every {
            totrinnskontrollRepository.update(capture(oppdaterSlot))
        } answers { firstArg() }

        totrinnskontrollService.lagreTotrinnskontrollOgReturnerSaksbehandler(
            saksbehandling(status = BehandlingStatus.UTREDES),
            BeslutteVedtakDto(false, "manglende", årsakerUnderkjent = listOf(ÅrsakUnderkjent.AKTIVITET)),
        )
        assertThat(oppdaterSlot.captured.årsakerUnderkjent?.årsaker!!).containsExactly(ÅrsakUnderkjent.AKTIVITET)
    }

    private fun totrinnskontroll(
        opprettetAv: String,
        status: TotrinnInternStatus,
    ) =
        Totrinnskontroll(
            behandlingId = UUID.randomUUID(),
            sporbar = Sporbar(opprettetAv),
            status = status,
            saksbehandler = opprettetAv,
        )

    private fun totrinnskontrollMedbeslutter(
        opprettetAv: String,
        beslutter: String,
    ) =
        Totrinnskontroll(
            behandlingId = UUID.randomUUID(),
            sporbar = Sporbar(opprettetAv),
            status = TotrinnInternStatus.KAN_FATTE_VEDTAK,
            saksbehandler = opprettetAv,
            beslutter = beslutter,
        )

    private fun totrinnskontrollMedbeslutterAArsakogBegrunnelse(
        opprettetAv: String,
        beslutter: String,

    ) =
        Totrinnskontroll(
            behandlingId = UUID.randomUUID(),
            sporbar = Sporbar(opprettetAv),
            status = TotrinnInternStatus.UNDERKJENT,
            saksbehandler = opprettetAv,
            beslutter = beslutter,
            årsakerUnderkjent = Årsaker(listOf(ÅrsakUnderkjent.INNGANGSVILKÅR_FORUTGÅENDE_MEDLEMSKAP_OPPHOLD, ÅrsakUnderkjent.VEDTAK_OG_BEREGNING)),
            begrunnelse = "begrunnelse underkjent",
        )

    private fun behandling(status: BehandlingStatus) = behandling(fagsak, status, steg = StegType.BESLUTTE_VEDTAK)

    companion object {

        private val ID = UUID.randomUUID()
        private val fagsak = fagsak()
    }
}
