package no.nav.tilleggsstonader.sak.klage.dto

import no.nav.tilleggsstonader.kontrakter.klage.KlagebehandlingDto

data class KlagebehandlingerDto(
    val overgangsstønad: List<KlagebehandlingDto>,
    val barnetilsyn: List<KlagebehandlingDto>,
    val skolepenger: List<KlagebehandlingDto>,
)
