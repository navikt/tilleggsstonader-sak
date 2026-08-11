package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingMetode
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import org.springframework.stereotype.Service

@Service
class SimuleringSteg(
    private val simuleringService: SimuleringService,
    private val vedtakService: VedtakService,
    private val tilkjentYtelseService: TilkjentYtelseService,
) : BehandlingSteg<Void?> {
    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        if (skalUtføreSimulering(saksbehandling)) {
            simuleringService.hentOgLagreSimuleringsresultat(saksbehandling)
        }
    }

    private fun skalUtføreSimulering(saksbehandling: Saksbehandling): Boolean {
        val typeVedtak = vedtakService.hentVedtaksresultat(saksbehandling)

        return when (typeVedtak) {
            TypeVedtak.INNVILGELSE,
            TypeVedtak.OPPHØR,
            -> harAndelerPåBehandlingEllerForrigeIverksatte(saksbehandling)
            TypeVedtak.AVSLAG -> false
        }
    }

    private fun harAndelerPåBehandlingEllerForrigeIverksatte(saksbehandling: Saksbehandling): Boolean {
        val harAndelerPåBehandling =
            tilkjentYtelseService.hentForBehandling(saksbehandling.id).andelerTilkjentYtelse.isNotEmpty()
        val harAndelerPåForrigeIverksatteBehandling =
            saksbehandling.forrigeIverksatteBehandlingId
                ?.let { tilkjentYtelseService.hentForBehandlingEllerNull(it) }
                ?.andelerTilkjentYtelse
                ?.isNotEmpty() ?: false

        return harAndelerPåBehandling || harAndelerPåForrigeIverksatteBehandling
    }

    override fun stegType(): StegType = StegType.SIMULERING

    override fun nesteSteg(saksbehandling: Saksbehandling): StegType =
        when {
            saksbehandling.erKjørelisteBehandling() &&
                saksbehandling.behandlingMetode == BehandlingMetode.MANUELL -> StegType.SEND_TIL_BESLUTTER
            saksbehandling.erKjørelisteBehandling() -> StegType.FULLFØR_KJØRELISTE
            else -> StegType.SEND_TIL_BESLUTTER
        }
}
