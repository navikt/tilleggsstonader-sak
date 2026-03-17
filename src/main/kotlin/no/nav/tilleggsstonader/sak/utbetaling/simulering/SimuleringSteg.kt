package no.nav.tilleggsstonader.sak.utbetaling.simulering

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
            TypeVedtak.INNVILGELSE -> tilkjentYtelseService.hentForBehandling(saksbehandling.id).andelerTilkjentYtelse.isNotEmpty()
            TypeVedtak.AVSLAG -> false
            TypeVedtak.OPPHØR -> true
        }
    }

    override fun stegType(): StegType = StegType.SIMULERING

    override fun nesteSteg(saksbehandling: Saksbehandling, kanBehandlePrivatBil: Boolean): StegType {
        return if (saksbehandling.erKjørelisteBehandling()) {
            StegType.FULLFØR_KJØRELISTE
        } else {
            StegType.SEND_TIL_BESLUTTER
        }
    }
}
