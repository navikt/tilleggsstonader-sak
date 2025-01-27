package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import org.springframework.stereotype.Service

@Service
class HarBehandlingUnderArbeidService(
    private val fagsakService: FagsakService,
) {
    fun harSøknadUnderBehandling(identStønadstype: IdentStønadstype): Boolean {
        val behandlinger =
            fagsakService.hentBehandlingerForPersonOgStønadstype(
                identStønadstype.ident,
                identStønadstype.stønadstype,
            )
        return behandlinger.any { erSøknadUnderBehandling(it.behandlingsårsak, it.status) }
    }

    companion object {
        private val statuserUnderBehandling =
            listOf(
                BehandlingStatus.OPPRETTET,
                BehandlingStatus.UTREDES,
                BehandlingStatus.FATTER_VEDTAK,
                BehandlingStatus.SATT_PÅ_VENT,
            )

        fun erSøknadUnderBehandling(
            årsak: BehandlingÅrsak,
            status: BehandlingStatus,
        ): Boolean = årsak == BehandlingÅrsak.SØKNAD && status in statuserUnderBehandling
    }
}
