package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Optional

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
        every { vedtakRepository.findById(behandlingId) } returns Optional.of(innvilgelseVedtak(behandlingId, true))

        val harRammevedtak =
            dagligReiseVedtakService.harRammevedtakPåDenneEllerForrgieBehandling(
                behandlingId = behandlingId,
                forrigeIverksatteBehandlingId = null,
            )

        assertThat(harRammevedtak).isTrue
    }

    @Test
    fun `skal returnere true når forrige behandling har rammevedtak`() {
        val behandlingId = BehandlingId.random()
        val forrigeBehandlingId = BehandlingId.random()
        every { vedtakRepository.findById(behandlingId) } returns Optional.empty()
        every { vedtakRepository.findById(forrigeBehandlingId) } returns Optional.of(innvilgelseVedtak(forrigeBehandlingId, true))

        val harRammevedtak =
            dagligReiseVedtakService.harRammevedtakPåDenneEllerForrgieBehandling(
                behandlingId = behandlingId,
                forrigeIverksatteBehandlingId = forrigeBehandlingId,
            )

        assertThat(harRammevedtak).isTrue
    }

    @Test
    fun `skal returnere true når forrige behandling er opphør med rammevedtak`() {
        val behandlingId = BehandlingId.random()
        val forrigeBehandlingId = BehandlingId.random()
        every { vedtakRepository.findById(behandlingId) } returns Optional.empty()
        every { vedtakRepository.findById(forrigeBehandlingId) } returns Optional.of(opphørVedtak(forrigeBehandlingId, true))

        val harRammevedtak =
            dagligReiseVedtakService.harRammevedtakPåDenneEllerForrgieBehandling(
                behandlingId = behandlingId,
                forrigeIverksatteBehandlingId = forrigeBehandlingId,
            )

        assertThat(harRammevedtak).isTrue
    }

    @Test
    fun `skal returnere false når nåværende behandling ikke har vedtak ennå og forrige ikke finnes`() {
        val behandlingId = BehandlingId.random()
        every { vedtakRepository.findById(behandlingId) } returns Optional.empty()

        val harRammevedtak =
            dagligReiseVedtakService.harRammevedtakPåDenneEllerForrgieBehandling(
                behandlingId = behandlingId,
                forrigeIverksatteBehandlingId = null,
            )

        assertThat(harRammevedtak).isFalse
    }

    @Test
    fun `skal returnere false når verken nåværende eller forrige har rammevedtak`() {
        val behandlingId = BehandlingId.random()
        val forrigeBehandlingId = BehandlingId.random()
        every { vedtakRepository.findById(behandlingId) } returns Optional.of(avslagVedtak(behandlingId))
        every { vedtakRepository.findById(forrigeBehandlingId) } returns Optional.of(innvilgelseVedtak(forrigeBehandlingId, false))

        val harRammevedtak =
            dagligReiseVedtakService.harRammevedtakPåDenneEllerForrgieBehandling(
                behandlingId = behandlingId,
                forrigeIverksatteBehandlingId = forrigeBehandlingId,
            )

        assertThat(harRammevedtak).isFalse
    }

    private fun innvilgelseVedtak(
        behandlingId: BehandlingId,
        harRammevedtak: Boolean,
    ): GeneriskVedtak<InnvilgelseDagligReise> =
        GeneriskVedtak(
            behandlingId = behandlingId,
            data =
                InnvilgelseDagligReise(
                    beregningsresultat = tomtBeregningsresultat(),
                    rammevedtakPrivatBil = if (harRammevedtak) rammevedtakPrivatBil() else null,
                    vedtaksperioder = emptyList(),
                    beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                ),
            gitVersjon = null,
            tidligsteEndring = null,
        )

    private fun opphørVedtak(
        behandlingId: BehandlingId,
        harRammevedtak: Boolean,
    ): GeneriskVedtak<OpphørDagligReise> =
        GeneriskVedtak(
            behandlingId = behandlingId,
            data =
                OpphørDagligReise(
                    vedtaksperioder = emptyList(),
                    beregningsresultat = tomtBeregningsresultat(),
                    rammevedtakPrivatBil = if (harRammevedtak) rammevedtakPrivatBil() else null,
                    årsaker = listOf(ÅrsakOpphør.ANNET),
                    begrunnelse = "begrunnelse",
                    beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                ),
            gitVersjon = null,
            tidligsteEndring = null,
        )

    private fun avslagVedtak(behandlingId: BehandlingId): GeneriskVedtak<AvslagDagligReise> =
        GeneriskVedtak(
            behandlingId = behandlingId,
            data =
                AvslagDagligReise(
                    årsaker = listOf(ÅrsakAvslag.ANNET),
                    begrunnelse = "begrunnelse",
                ),
            gitVersjon = null,
            tidligsteEndring = null,
        )

    private fun tomtBeregningsresultat() =
        BeregningsresultatDagligReise(
            offentligTransport = null,
            privatBil = null,
        )
}
