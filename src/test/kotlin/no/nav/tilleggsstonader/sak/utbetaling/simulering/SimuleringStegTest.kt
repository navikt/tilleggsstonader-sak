package no.nav.tilleggsstonader.sak.utbetaling.simulering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SimuleringStegTest {
    val simuleringService = mockk<SimuleringService>()
    val vedtakService = mockk<VedtakService>()
    val tilkjentYtelseSerivce = mockk<TilkjentYtelseService>()

    val simuleringSteg =
        SimuleringSteg(simuleringService, vedtakService, tilkjentYtelseSerivce)

    @BeforeEach
    fun setUp() {
        every { simuleringService.hentOgLagreSimuleringsresultat(any()) } returns mockk()
    }

    private fun mockVedtakMedType(type: TypeVedtak) = every { vedtakService.hentVedtaksresultat(any()) } returns type

    @Nested
    inner class Revurdering {
        @Test
        fun `skal utføre simulering for innvilget revurdering`() {
            val saksbehandling =
                saksbehandling(
                    type = BehandlingType.REVURDERING,
                )
            every { tilkjentYtelseSerivce.hentForBehandling(saksbehandling.id) } returns tilkjentYtelse(saksbehandling.id)

            mockVedtakMedType(TypeVedtak.INNVILGELSE)

            simuleringSteg.utførSteg(saksbehandling, null)

            verify { simuleringService.hentOgLagreSimuleringsresultat(saksbehandling) }
        }

        @Test
        fun `skal ikke utføre simulering for avslått revurdering`() {
            val saksbehandling =
                saksbehandling(
                    type = BehandlingType.REVURDERING,
                )
            mockVedtakMedType(TypeVedtak.AVSLAG)

            simuleringSteg.utførSteg(saksbehandling, null)

            verify(exactly = 0) { simuleringService.hentOgLagreSimuleringsresultat(saksbehandling) }
        }

        @Test
        fun `skal utføre simulering for opphør når forrige iverksatte behandling har andeler`() {
            val forrigeIverksatteBehandlingId = BehandlingId.random()
            val saksbehandling =
                saksbehandling(
                    type = BehandlingType.REVURDERING,
                    forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId,
                )
            every { tilkjentYtelseSerivce.hentForBehandling(saksbehandling.id) } returns
                tilkjentYtelse(
                    saksbehandling.id,
                ).copy(andelerTilkjentYtelse = emptySet())
            every { tilkjentYtelseSerivce.hentForBehandlingEllerNull(forrigeIverksatteBehandlingId) } returns
                tilkjentYtelse(forrigeIverksatteBehandlingId, andelTilkjentYtelse(statusIverksetting = StatusIverksetting.OK))
            mockVedtakMedType(TypeVedtak.OPPHØR)

            simuleringSteg.utførSteg(saksbehandling, null)

            verify { simuleringService.hentOgLagreSimuleringsresultat(saksbehandling) }
        }

        @Test
        fun `skal ikke utføre simulering for opphør når forrige iverksatte behandling har andeler som ikke er sendt til økonomi`() {
            val forrigeIverksatteBehandlingId = BehandlingId.random()
            val saksbehandling =
                saksbehandling(
                    type = BehandlingType.REVURDERING,
                    forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId,
                )
            every { tilkjentYtelseSerivce.hentForBehandling(saksbehandling.id) } returns
                tilkjentYtelse(
                    saksbehandling.id,
                ).copy(andelerTilkjentYtelse = emptySet())
            every { tilkjentYtelseSerivce.hentForBehandlingEllerNull(forrigeIverksatteBehandlingId) } returns
                tilkjentYtelse(forrigeIverksatteBehandlingId, andelTilkjentYtelse(statusIverksetting = StatusIverksetting.UBEHANDLET))
            mockVedtakMedType(TypeVedtak.OPPHØR)

            simuleringSteg.utførSteg(saksbehandling, null)

            verify(exactly = 0) { simuleringService.hentOgLagreSimuleringsresultat(saksbehandling) }
        }

        @Test
        fun `skal ikke utføre simulering ved innvilgelse når forrige iverksatte behandling har andeler som ikke er sendt til økonomi`() {
fun `skal ikke utføre simulering for innvilget revurdering når forrige iverksatte behandling har andeler som ikke er sendt til økonomi`() {
            val forrigeIverksatteBehandlingId = BehandlingId.random()
            val saksbehandling =
                saksbehandling(
                    type = BehandlingType.REVURDERING,
                    forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId,
                )
            every { tilkjentYtelseSerivce.hentForBehandling(saksbehandling.id) } returns
                tilkjentYtelse(
                    saksbehandling.id,
                ).copy(andelerTilkjentYtelse = emptySet())
            every { tilkjentYtelseSerivce.hentForBehandlingEllerNull(forrigeIverksatteBehandlingId) } returns
                tilkjentYtelse(forrigeIverksatteBehandlingId, andelTilkjentYtelse(statusIverksetting = StatusIverksetting.UBEHANDLET))
            mockVedtakMedType(TypeVedtak.INNVILGELSE)

            simuleringSteg.utførSteg(saksbehandling, null)

            verify(exactly = 0) { simuleringService.hentOgLagreSimuleringsresultat(saksbehandling) }
        }

        @Test
        fun `skal ikke utføre simulering for opphør uten andeler på behandling eller forrige iverksatte`() {
            val forrigeIverksatteBehandlingId = BehandlingId.random()
            val saksbehandling =
                saksbehandling(
                    type = BehandlingType.REVURDERING,
                    forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId,
                )
            every { tilkjentYtelseSerivce.hentForBehandling(saksbehandling.id) } returns
                tilkjentYtelse(
                    saksbehandling.id,
                ).copy(andelerTilkjentYtelse = emptySet())
            every { tilkjentYtelseSerivce.hentForBehandlingEllerNull(forrigeIverksatteBehandlingId) } returns
                tilkjentYtelse(
                    forrigeIverksatteBehandlingId,
                ).copy(andelerTilkjentYtelse = emptySet())
            mockVedtakMedType(TypeVedtak.OPPHØR)

            simuleringSteg.utførSteg(saksbehandling, null)

            verify(exactly = 0) { simuleringService.hentOgLagreSimuleringsresultat(saksbehandling) }
        }
    }

    @Nested
    inner class Førstegangsbehandling {
        @Test
        fun `skal utføre simulering for innvilget førstegangsbehandling`() {
            val saksbehandling =
                saksbehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                )
            every { tilkjentYtelseSerivce.hentForBehandling(saksbehandling.id) } returns tilkjentYtelse(saksbehandling.id)
            mockVedtakMedType(TypeVedtak.INNVILGELSE)

            simuleringSteg.utførSteg(saksbehandling, null)

            verify { simuleringService.hentOgLagreSimuleringsresultat(saksbehandling) }
        }

        @Test
        fun `skal ikke utføre simulering for avslått førstegangsbehandling`() {
            val saksbehandling =
                saksbehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                )
            mockVedtakMedType(TypeVedtak.AVSLAG)

            simuleringSteg.utførSteg(saksbehandling, null)

            verify(exactly = 0) { simuleringService.hentOgLagreSimuleringsresultat(saksbehandling) }
        }

        @Test
        fun `skal ikke utføre simulering dersom det er et rammevedtak`() {
            val saksbehandling =
                saksbehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                )
            every { tilkjentYtelseSerivce.hentForBehandling(saksbehandling.id) } returns
                tilkjentYtelse(
                    saksbehandling.id,
                ).copy(andelerTilkjentYtelse = emptySet())

            mockVedtakMedType(TypeVedtak.INNVILGELSE)

            simuleringSteg.utførSteg(saksbehandling, null)

            verify(exactly = 0) { simuleringService.hentOgLagreSimuleringsresultat(saksbehandling) }
        }

        @Test
        fun `skal utføre simulering ved innvilgelse når forrige iverksatte behandling har andeler`() {
            val forrigeIverksatteBehandlingId = BehandlingId.random()
            val saksbehandling =
                saksbehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId,
                )
            every { tilkjentYtelseSerivce.hentForBehandling(saksbehandling.id) } returns
                tilkjentYtelse(
                    saksbehandling.id,
                ).copy(andelerTilkjentYtelse = emptySet())
            every { tilkjentYtelseSerivce.hentForBehandlingEllerNull(forrigeIverksatteBehandlingId) } returns
                tilkjentYtelse(forrigeIverksatteBehandlingId, andelTilkjentYtelse(statusIverksetting = StatusIverksetting.OK))
            mockVedtakMedType(TypeVedtak.INNVILGELSE)

            simuleringSteg.utførSteg(saksbehandling, null)

            verify { simuleringService.hentOgLagreSimuleringsresultat(saksbehandling) }
        }
    }
}
