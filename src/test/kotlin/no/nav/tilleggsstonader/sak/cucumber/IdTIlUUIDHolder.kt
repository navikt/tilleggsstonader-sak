package no.nav.tilleggsstonader.sak.cucumber

import java.util.UUID

object IdTIlUUIDHolder {

    val behandlingIdTilUUID = (1..10).associateWith { UUID.randomUUID() }

    fun behandlingIdFraUUID(id: UUID) = behandlingIdTilUUID.entries.single { it.value == id }.key

    /**
     * behandlingId to ident, to barnId
     */
    val barnIder = (1..10).associateWith { UUID.randomUUID() }

    fun barnIdFraUUID(id: UUID) = behandlingIdTilUUID.entries.single { it.value == id }.key

}
