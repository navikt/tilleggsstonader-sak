package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.kontrakter.felles.gjelderDagligReise
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
) : BehandlingSteg<Void?> {
    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        if (skalUtFøreSimulering(saksbehandling)) {
            simuleringService.hentOgLagreSimuleringsresultat(saksbehandling)
        }
    }

    private fun skalUtFøreSimulering(saksbehandling: Saksbehandling): Boolean {
        if (saksbehandling.stønadstype.gjelderDagligReise()) {
            return false
        }

        val typeVedtak = vedtaksresultatService.hentVedtaksresultat(saksbehandling)
        return when (typeVedtak) {
            TypeVedtak.INNVILGELSE -> true
            TypeVedtak.AVSLAG -> false
            TypeVedtak.OPPHØR -> true
        }
    }

    override fun stegType(): StegType = StegType.SIMULERING
}
