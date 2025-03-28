package no.nav.tilleggsstonader.sak.behandling.dto

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import java.time.LocalDate

data class OpprettBehandlingDto(
    val fagsakId: FagsakId,
    val årsak: BehandlingÅrsak,
    val valgteBarn: Set<String> = emptySet(),
    val kravMottatt: LocalDate?,
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
