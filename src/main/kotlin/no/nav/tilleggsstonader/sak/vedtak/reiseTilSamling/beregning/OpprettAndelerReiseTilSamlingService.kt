package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørReiseTilSamling

class OpprettAndelerReiseTilSamlingService(
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val vedtakService: VedtakService,
) {
    fun lagreAndelerForBehandling(saksbehandling: Saksbehandling) {
        val vedtak = vedtakService.hentVedtak<InnvilgelseEllerOpphørReiseTilSamling>(saksbehandling.id)

        val andelerOffentligTransport =
            vedtak.data.beregningsresultat.offentligTransport
                ?.mapTilAndelTilkjentYtelse(saksbehandling)
                ?: emptyList()

        val andelerPrivatBil =
            vedtak.data.beregningsresultat.privatBil
                ?.mapTilAndelTilkjentYtelse(saksbehandling)
                ?: emptyList()

        tilkjentYtelseService.lagreTilkjentYtelse(
            behandlingId = saksbehandling.id,
            andeler = andelerOffentligTransport + andelerPrivatBil,
        )
    }
}
