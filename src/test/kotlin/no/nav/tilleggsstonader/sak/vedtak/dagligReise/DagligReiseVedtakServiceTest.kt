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
    fun `skal returnere true når nåværende behandling har rammevedtak`() {
        val behandlingId = BehandlingId.random()
        every { vedtakRepository.harRammevedtak(listOf(behandlingId)) } returns true

        val harRammevedtak =
            dagligReiseVedtakService.harRammevedtakPåDenneEllerForrgieBehandling(
                behandlingId = behandlingId,
                forrigeIverksatteBehandlingId = null,
            )

        assertThat(harRammevedtak).isTrue
    }

    @Test
    fun `skal returnere true når en av behandlingene har rammevedtak`() {
        val behandlingId = BehandlingId.random()
        val forrigeBehandlingId = BehandlingId.random()
        every { vedtakRepository.harRammevedtak(listOf(behandlingId, forrigeBehandlingId)) } returns true

        val harRammevedtak =
            dagligReiseVedtakService.harRammevedtakPåDenneEllerForrgieBehandling(
                behandlingId = behandlingId,
                forrigeIverksatteBehandlingId = forrigeBehandlingId,
            )

        assertThat(harRammevedtak).isTrue
    }

    @Test
    fun `skal returnere false når ingen behandlinger har rammevedtak`() {
        val behandlingId = BehandlingId.random()
        val forrigeBehandlingId = BehandlingId.random()
        every { vedtakRepository.harRammevedtak(listOf(behandlingId, forrigeBehandlingId)) } returns false

        val harRammevedtak =
            dagligReiseVedtakService.harRammevedtakPåDenneEllerForrgieBehandling(
                behandlingId = behandlingId,
                forrigeIverksatteBehandlingId = forrigeBehandlingId,
            )

        assertThat(harRammevedtak).isFalse
    }

    @Test
    fun `skal returnere false når det ikke finnes forrige behandling og nåværende ikke har rammevedtak`() {
        val behandlingId = BehandlingId.random()
        every { vedtakRepository.harRammevedtak(listOf(behandlingId)) } returns false

        val harRammevedtak =
            dagligReiseVedtakService.harRammevedtakPåDenneEllerForrgieBehandling(
                behandlingId = behandlingId,
                forrigeIverksatteBehandlingId = null,
            )

        assertThat(harRammevedtak).isFalse
    }
}
