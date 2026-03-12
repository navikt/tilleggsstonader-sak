package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.mapTilAndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import org.springframework.stereotype.Service

@Service
class OpprettAndelerDagligReiseService(
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val vedtakService: VedtakService,
) {
    fun lagreAndelerForBehandling(saksbehandling: Saksbehandling) {
        val vedtak = vedtakService.hentVedtak<InnvilgelseEllerOpphørDagligReise>(saksbehandling.id)

        val andelerOffentligTransport =
            vedtak.data.beregningsresultat.offentligTransport
                ?.mapTilAndelTilkjentYtelse(saksbehandling)
                ?: emptyList()

        val rammevedtak = vedtak.data.rammevedtakPrivatBil
        val andelerPrivatBil =
            if (rammevedtak != null) {
                vedtak.data.beregningsresultat.privatBil
                    ?.mapTilAndelTilkjentYtelse(saksbehandling, rammevedtak)
                    ?: emptyList()
            } else {
                emptyList()
            }

        tilkjentYtelseService.lagreTilkjentYtelse(
            behandlingId = saksbehandling.id,
            andeler = andelerOffentligTransport + andelerPrivatBil,
        )
    }
}
