package no.nav.tilleggsstonader.sak.vedtak.flytting

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.BeregningsplanUtleder
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagFlytting
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.flytting.beregning.FlyttingBeregningService
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.AvslagFlyttingDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlyttingBeregnYtelseStegTest {
    private val vedtakRepository = mockk<VedtakRepository>(relaxed = true)
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val steg =
        FlyttingBeregnYtelseSteg(
            beregningService = mockk<FlyttingBeregningService>(),
            beregningsplanUtleder = mockk<BeregningsplanUtleder>(),
            vedtakRepository = vedtakRepository,
            tilkjentYtelseService = tilkjentYtelseService,
            simuleringService = mockk<SimuleringService>(relaxed = true),
        )

    @Test
    fun `avslag lagres uten å opprette tilkjent ytelse`() {
        val vedtak = slot<GeneriskVedtak<*>>()
        val behandling = saksbehandling(fagsak = fagsak(stønadstype = Stønadstype.FLYTTING_TSO))
        every { vedtakRepository.insert(capture(vedtak)) } answers { firstArg() }

        steg.utførSteg(
            saksbehandling = behandling,
            data =
                AvslagFlyttingDto(
                    årsakerAvslag = listOf(ÅrsakAvslag.LØNN_I_TILTAK),
                    begrunnelse = "begrunnelse",
                ),
        )

        assertThat(vedtak.captured.type).isEqualTo(TypeVedtak.AVSLAG)
        assertThat(vedtak.captured.data)
            .isEqualTo(AvslagFlytting(årsaker = listOf(ÅrsakAvslag.LØNN_I_TILTAK), begrunnelse = "begrunnelse"))
        verify(exactly = 0) { tilkjentYtelseService.lagreTilkjentYtelse(any(), any()) }
    }
}
