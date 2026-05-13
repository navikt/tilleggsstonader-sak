package no.nav.tilleggsstonader.sak.behandling.opprettelse

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.sisteFerdigstilteBehandling
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType.KJØRELISTE
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType.REVURDERING
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import org.springframework.http.HttpStatus

object OpprettBehandlingUtil {
    /**
     * @param behandlingType for ny behandling
     */
    fun validerKanOppretteNyBehandling(
        stønadstype: Stønadstype,
        behandlingType: BehandlingType,
        tidligereBehandlinger: List<Behandling>,
        sisteIverksatteBehandling: Behandling?,
        sisteIverksatteBehandlingHarRammevedtakForPrivatBil: Boolean? = null,
    ) {
        val sisteFerdigstilteBehandling =
            tidligereBehandlinger
                .filter { it.resultat != BehandlingResultat.HENLAGT }
                .sisteFerdigstilteBehandling()

        when (behandlingType) {
            FØRSTEGANGSBEHANDLING -> validerKanOppretteFørstegangsbehandling(sisteFerdigstilteBehandling)
            REVURDERING -> validerKanOppretteRevurdering(sisteFerdigstilteBehandling)
            KJØRELISTE ->
                validerKanOppretteKjørelisteBehandling(
                    sisteIverksatteBehandling = sisteIverksatteBehandling,
                    stønadstype = stønadstype,
                    sisteIverksatteBehandlingHarRammevedtakForPrivatBil =
                    sisteIverksatteBehandlingHarRammevedtakForPrivatBil,
                )
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

    // TODO: Bør det valideres noe mer? F.eks. at det finnes en kjøreliste på forrige behandling?
    private fun validerKanOppretteKjørelisteBehandling(
        sisteIverksatteBehandling: Behandling?,
        stønadstype: Stønadstype,
        sisteIverksatteBehandlingHarRammevedtakForPrivatBil: Boolean?,
    ) {
        brukerfeilHvis(stønadstype != Stønadstype.DAGLIG_REISE_TSO && stønadstype != Stønadstype.DAGLIG_REISE_TSR) {
            "Det er ikke lov å opprette en kjørelistebehandling på stønadstype $stønadstype"
        }

        brukerfeilHvis(sisteIverksatteBehandling == null) {
            "Det finnes ikke en tidligere iverksatt behandling på fagsaken"
        }

        brukerfeilHvis(sisteIverksatteBehandlingHarRammevedtakForPrivatBil == false) {
            "Siste iverksatte behandling mangler rammevedtak for privat bil"
        }
    }
}
