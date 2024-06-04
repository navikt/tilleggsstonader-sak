package no.nav.tilleggsstonader.sak.klage.dto

import no.nav.tilleggsstonader.kontrakter.klage.KlagebehandlingDto

data class KlagebehandlingerDto(
    val barnetilsyn: List<KlagebehandlingDto>,
)
