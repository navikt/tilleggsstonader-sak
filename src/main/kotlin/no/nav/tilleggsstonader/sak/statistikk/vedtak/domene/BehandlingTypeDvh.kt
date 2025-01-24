package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType

enum class BehandlingTypeDvh {
    FØRSTEGANGSBEHANDLING,
    REVURDERING,
    ;

    companion object {
        fun fraDomene(type: BehandlingType) = when (type) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> FØRSTEGANGSBEHANDLING
            BehandlingType.REVURDERING -> REVURDERING
        }
    }
}
