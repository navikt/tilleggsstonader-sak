package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.mapTilAndelTilkentYtelse
import org.springframework.stereotype.Service

@Service
class OpprettAndelerReiseTilSamlingService(
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val vedtakService: VedtakService,
) {
    fun lagreAndelerForBehandling(saksbehandling: Saksbehandling) {
        val vedtak = vedtakService.hentVedtak<InnvilgelseEllerOpphørReiseTilSamling>(saksbehandling.id)
        tilkjentYtelseService.lagreTilkjentYtelse(
            behandlingId = saksbehandling.id,
            andeler = vedtak.data.beregningsresultat.mapTilAndelTilkentYtelse(saksbehandling) ?: emptyList(),
        )
    }
}
