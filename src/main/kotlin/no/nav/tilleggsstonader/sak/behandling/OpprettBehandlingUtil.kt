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
        kanHaFlereBehandlingPåSammeFagsak: Boolean = false,
    ) {
        val sisteFerdigstilteBehandling =
            tidligereBehandlinger
                .filter { it.resultat != BehandlingResultat.HENLAGT }
                .sisteFerdigstilteBehandling()

        if (kanHaFlereBehandlingPåSammeFagsak) {
            validerTidligereBehandlingerErFerdigstilteEllerPåVent(tidligereBehandlinger)
        } else {
            validerTidligereBehandlingerErFerdigstilte(tidligereBehandlinger)
        }

        when (behandlingType) {
            FØRSTEGANGSBEHANDLING -> validerKanOppretteFørstegangsbehandling(sisteFerdigstilteBehandling)
            REVURDERING -> validerKanOppretteRevurdering(sisteFerdigstilteBehandling)
        }
    }

    // TODO: Slett når snike i køen er implementert
    private fun validerTidligereBehandlingerErFerdigstilte(tidligereBehandlinger: List<Behandling>) {
        if (tidligereBehandlinger.any { it.status != BehandlingStatus.FERDIGSTILT }) {
            throw ApiFeil("Det finnes en behandling på fagsaken som ikke er ferdigstilt", HttpStatus.BAD_REQUEST)
        }
        feilHvis(tidligereBehandlinger.any { it.type == FØRSTEGANGSBEHANDLING && it.status == BehandlingStatus.SATT_PÅ_VENT }) {
            "Kan ikke opprette ny behandling når det finnes en førstegangsbehandling på vent"
        }
    }

    private fun validerTidligereBehandlingerErFerdigstilteEllerPåVent(tidligereBehandlinger: List<Behandling>) {
        brukerfeilHvis(tidligereBehandlinger.any { it.erAktiv() }) {
            "Det finnes en behandling på fagsaken som hverken er ferdigstilt eller satt på vent"
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
