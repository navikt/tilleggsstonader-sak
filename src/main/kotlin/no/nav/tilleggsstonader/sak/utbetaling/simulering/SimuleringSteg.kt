package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import org.springframework.stereotype.Service

@Service
class SimuleringSteg(
    val simuleringService: SimuleringService,
) : BehandlingSteg<Void?> {

    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        if (skalUtFøreSimulering(saksbehandling)) {
            simuleringService.hentOgLagreSimuleringsresultat(saksbehandling)
        }
    }

    private fun skalUtFøreSimulering(saksbehandling: Saksbehandling): Boolean =
        when (saksbehandling.type) {
            BehandlingType.REVURDERING -> resultatSkalSimuleres(saksbehandling.resultat)
            BehandlingType.FØRSTEGANGSBEHANDLING -> false
            else -> error("Behandlingstype ${saksbehandling.type} støttes ikke i simulering")
        }

    private fun resultatSkalSimuleres(resultat: BehandlingResultat): Boolean {
        return when (resultat) {
            // TODO: Bruke resultat.skalIverksettes ?
            BehandlingResultat.INNVILGET -> true
            BehandlingResultat.OPPHØRT -> true
            BehandlingResultat.AVSLÅTT -> false
            else -> error("Simulering støtter ikke resultat $resultat")
        }
    }

    override fun stegType(): StegType = StegType.SIMULERING
}
