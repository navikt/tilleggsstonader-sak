package no.nav.tilleggsstonader.sak.cucumber

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId

object IdTIlUUIDHolder {
    val behandlingIdTilUUID = (1..10).associateWith { BehandlingId.random() }

    fun behandlingIdFraUUID(id: BehandlingId) = behandlingIdTilUUID.entries.single { it.value == id }.key

    /**
     * behandlingId to ident, to barnId
     */
    val barnIder = (1..10).associateWith { BarnId.random() }

    fun barnIdFraUUID(id: BarnId) = barnIder.entries.single { it.value == id }.key
}
