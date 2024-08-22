package no.nav.tilleggsstonader.sak.behandling.dto

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import java.util.UUID

data class OpprettBehandlingDto(
    val fagsakId: UUID,
    val behandlingsårsak: BehandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER, // TODO slett default når frontend sender med
)
