package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.sisteFerdigstilteBehandling
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType.REVURDERING
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import org.springframework.http.HttpStatus

object OpprettBehandlingUtil {
    /**
     * @param behandlingType for ny behandling
     */
    fun validerKanOppretteNyBehandling(
        behandlingType: BehandlingType,
        tidligereBehandlinger: List<Behandling>,
    ) {
        val sisteFerdigstilteBehandling =
            tidligereBehandlinger
                .filter { it.resultat != BehandlingResultat.HENLAGT }
                .sisteFerdigstilteBehandling()

        when (behandlingType) {
            FØRSTEGANGSBEHANDLING -> validerKanOppretteFørstegangsbehandling(sisteFerdigstilteBehandling)
            REVURDERING -> validerKanOppretteRevurdering(sisteFerdigstilteBehandling)
        }
    }

    private fun validerKanOppretteFørstegangsbehandling(sisteFerdigstilteBehandling: Behandling?) {
        if (sisteFerdigstilteBehandling == null) return
        brukerfeilHvis(sisteFerdigstilteBehandling.type != FØRSTEGANGSBEHANDLING) {
            "Kan ikke opprette en førstegangsbehandling når forrige behandling ikke er en førstegangsbehandling"
        }
        brukerfeilHvis(sisteFerdigstilteBehandling.resultat != BehandlingResultat.HENLAGT) {
            "Kan ikke opprette en førstegangsbehandling når siste behandling ikke er henlagt"
        }
    }

    private fun validerKanOppretteRevurdering(sisteFerdigstilteBehandling: Behandling?) {
        if (sisteFerdigstilteBehandling == null) {
            throw ApiFeil("Det finnes ikke en tidligere behandling på fagsaken", HttpStatus.BAD_REQUEST)
        }
    }
}
