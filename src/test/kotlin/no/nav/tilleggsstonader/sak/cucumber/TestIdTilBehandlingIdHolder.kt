package no.nav.tilleggsstonader.sak.cucumber

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId

object TestIdTilBehandlingIdHolder {
    val testIdTilBehandlingId = (1..10).associateWith { BehandlingId.random() }

    fun behandlingIdTilTestId(id: BehandlingId) = testIdTilBehandlingId.entries.single { it.value == id }.key
}
