package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.felles.domain.BarnId

object BarnIdTilTestIdHolder {
    val barnIder = (1..10).associateWith { BarnId.random() }

    fun barnIdFraUUID(id: BarnId) = barnIder.entries.single { it.value == id }.key
}
