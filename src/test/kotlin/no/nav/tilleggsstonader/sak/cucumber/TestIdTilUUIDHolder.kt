package no.nav.tilleggsstonader.sak.cucumber

import java.util.UUID

object TestIdTilUUIDHolder {
    val testIdTilUUID = (1..10).associateWith { UUID.randomUUID() }

    fun uuidTilTestId(id: UUID) = testIdTilUUID.entries.single { it.value == id }.key
}
