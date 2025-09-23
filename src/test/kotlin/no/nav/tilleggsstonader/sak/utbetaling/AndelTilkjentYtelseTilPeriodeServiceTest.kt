package no.nav.tilleggsstonader.sak.utbetaling

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.interntVedtak.Testdata
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.nullAndel
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.mapTilAndelTilkjentYtelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AndelTilkjentYtelseTilPeriodeServiceTest {
    val tilkjentYtelseService = mockk<TilkjentYtelseService>()

    val vedtakservice = mockk<VedtakService>()
    val andelTilkjentYtelseTilPeriodeService = AndelTilkjentYtelseTilPeriodeService(tilkjentYtelseService, vedtakservice)

    @Test
    fun `mapAndelerTilVedtaksperiodeForBehandling, andel er nullandel, tom vedtaksperiode`() {
        val behandlingId = BehandlingId.random()
        every { tilkjentYtelseService.hentForBehandling(behandlingId) } returns
            tilkjentYtelse(behandlingId = behandlingId, andeler = arrayOf(nullAndel()))
        every { vedtakservice.hentVedtak(behandlingId) } returns Testdata.Boutgifter.innvilgetVedtak

        val response = andelTilkjentYtelseTilPeriodeService.mapAndelerTilVedtaksperiodeForBehandling(behandlingId)
        assertThat(response).hasSize(1)

        val mappetAndel = response.single()
        assertThat(mappetAndel.andelTilkjentYtelse.beløp).isEqualTo(0)
        assertThat(mappetAndel.vedtaksperiode).isNull()
    }

    @Test
    fun `mapAndelerTilVedtaksperiodeForBehandling, andeler mappes`() {
        val vedtak = Testdata.Boutgifter.innvilgetVedtak

        val behandlingId = BehandlingId.random()
        every { tilkjentYtelseService.hentForBehandling(behandlingId) } returns
            tilkjentYtelse(
                behandlingId = behandlingId,
                andeler =
                    vedtak.data.beregningsresultat
                        .mapTilAndelTilkjentYtelse(behandlingId)
                        .toTypedArray(),
            )
        every { vedtakservice.hentVedtak(behandlingId) } returns vedtak

        val response = andelTilkjentYtelseTilPeriodeService.mapAndelerTilVedtaksperiodeForBehandling(behandlingId)
        assertThat(response).hasSize(vedtak.data.beregningsresultat.perioder.size)
    }
}
