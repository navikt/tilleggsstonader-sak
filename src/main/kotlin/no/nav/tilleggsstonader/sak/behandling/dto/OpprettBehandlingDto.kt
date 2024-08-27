package no.nav.tilleggsstonader.sak.behandling.dto

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import java.util.UUID

data class OpprettBehandlingDto(
    val fagsakId: UUID,
    val årsak: BehandlingÅrsak,
    val valgteBarn: Set<String> = emptySet(),
)

data class BarnTilRevurderingDto(
    val barn: List<Barn>,
) {
    data class Barn(
        val ident: String,
        val navn: String,
        val finnesPåForrigeBehandling: Boolean,
    )
}
