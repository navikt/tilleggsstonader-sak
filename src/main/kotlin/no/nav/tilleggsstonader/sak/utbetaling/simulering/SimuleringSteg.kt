package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
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

    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        val resultat = vedtaksresultatService.hentVedtaksresultat(saksbehandling)

        if (skalUtFøreSimulering(saksbehandling, resultat)) {
            simuleringService.hentOgLagreSimuleringsresultat(saksbehandling)
        }
    }

    private fun skalUtFøreSimulering(saksbehandling: Saksbehandling, vedtak: TypeVedtak): Boolean =
        when (saksbehandling.type) {
            BehandlingType.REVURDERING -> resultatSkalSimuleres(vedtak)
            BehandlingType.FØRSTEGANGSBEHANDLING -> false
            else -> error("Behandlingstype ${saksbehandling.type} støttes ikke i simulering")
        }

    private fun resultatSkalSimuleres(type: TypeVedtak): Boolean {
        return when (type) {
            TypeVedtak.INNVILGELSE -> true
            TypeVedtak.AVSLAG -> false
            else -> error("Simulering støtter ikke resultat $type")
        }
    }

    override fun stegType(): StegType = StegType.SIMULERING
}
