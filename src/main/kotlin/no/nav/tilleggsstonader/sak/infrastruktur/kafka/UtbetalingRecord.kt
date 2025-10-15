package no.nav.tilleggsstonader.sak.infrastruktur.kafka

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId

data class UtbetalingRecord(
    val behandlingId: BehandlingId,
)
