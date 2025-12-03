package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtaksresultatService
import org.springframework.stereotype.Service

@Service
class SimuleringSteg(
    val simuleringService: SimuleringService,
    val vedtaksresultatService: VedtaksresultatService,
    val unleashService: UnleashService,
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
        val typeVedtak = vedtaksresultatService.hentVedtaksresultat(saksbehandling)
        return when (typeVedtak) {
            TypeVedtak.INNVILGELSE -> true
            TypeVedtak.AVSLAG -> false
            TypeVedtak.OPPHØR -> true
        }
    }

    override fun stegType(): StegType = StegType.SIMULERING
}
