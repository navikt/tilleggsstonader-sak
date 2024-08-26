package no.nav.tilleggsstonader.sak.behandling.dto

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import java.util.UUID

data class OpprettBehandlingDto(
    val fagsakId: UUID,
    val årsak: BehandlingÅrsak,
)
