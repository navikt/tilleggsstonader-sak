package no.nav.tilleggsstonader.sak.behandling.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId

data class EksternId(
    val behandlingId: BehandlingId,
    val eksternBehandlingId: Long,
    val eksternFagsakId: Long,
)
