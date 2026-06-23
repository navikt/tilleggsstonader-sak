package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DagligReiseVedtakServiceTest {
    private val vedtakRepository = mockk<VedtakRepository>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val simuleringService = mockk<SimuleringService>(relaxed = true)

    private val dagligReiseVedtakService =
        DagligReiseVedtakService(
            vedtakRepository = vedtakRepository,
            tilkjentYtelseService = tilkjentYtelseService,
            simuleringService = simuleringService,
        )

    @Test
    fun `skal returnere true når forrige behandling har rammevedtak`() {
        val forrigeBehandlingId = BehandlingId.random()
        every { vedtakRepository.harRammevedtak(listOf(forrigeBehandlingId)) } returns true

        val harRammevedtak =
            dagligReiseVedtakService.forrigeIverksatteBehandlingHarRammevedtakForPrivatBil(
                forrigeIverksatteBehandlingId = forrigeBehandlingId,
            )

        assertThat(harRammevedtak).isTrue
    }

    @Test
    fun `skal returnere false når forrige behandling ikke har rammevedtak`() {
        val forrigeBehandlingId = BehandlingId.random()
        every { vedtakRepository.harRammevedtak(listOf(forrigeBehandlingId)) } returns false

        val harRammevedtak =
            dagligReiseVedtakService.forrigeIverksatteBehandlingHarRammevedtakForPrivatBil(
                forrigeIverksatteBehandlingId = forrigeBehandlingId,
            )

        assertThat(harRammevedtak).isFalse
    }

    @Test
    fun `skal returnere false når det ikke finnes forrige behandling`() {
        val harRammevedtak =
            dagligReiseVedtakService.forrigeIverksatteBehandlingHarRammevedtakForPrivatBil(
                forrigeIverksatteBehandlingId = null,
            )

        assertThat(harRammevedtak).isFalse
    }
}
