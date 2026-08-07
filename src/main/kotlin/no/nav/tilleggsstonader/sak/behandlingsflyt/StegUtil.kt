package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak

object StegUtil {
    fun utledFørsteStegForBehandling(
        behandlingType: BehandlingType,
        behandlingsårsak: BehandlingÅrsak,
    ): StegType =
        when (behandlingType) {
            BehandlingType.KJØRELISTE -> utledStegtypeForKjørelistebehandling(behandlingsårsak)
            else -> StegType.INNGANGSVILKÅR
        }

    private fun utledStegtypeForKjørelistebehandling(behandlingsårsak: BehandlingÅrsak): StegType =
        when (behandlingsårsak) {
            BehandlingÅrsak.REGISTRER_KJØRELISTE_FOR_BRUKER -> StegType.REGISTRER_KJØRELISTE
            else -> StegType.KJØRELISTE
        }
}
