package no.nav.tilleggsstonader.sak.klage.dto

import no.nav.tilleggsstonader.kontrakter.klage.KlagebehandlingDto

data class KlagebehandlingerDto(
    // TODO TilsynBarn bør være PassAvBarn, men dette brukes eksternt
    val tilsynBarn: List<KlagebehandlingDto>,
    val læremidler: List<KlagebehandlingDto>,
    val boutgifter: List<KlagebehandlingDto>,
    val dagligReiseTso: List<KlagebehandlingDto>,
    val dagligReiseTsr: List<KlagebehandlingDto>,
    val reiseTilSamlingTso: List<KlagebehandlingDto>,
    val reiseTilSamlingTsr: List<KlagebehandlingDto>,
) {
    companion object {
        fun empty() =
            KlagebehandlingerDto(
                tilsynBarn = emptyList(),
                læremidler = emptyList(),
                boutgifter = emptyList(),
                dagligReiseTso = emptyList(),
                dagligReiseTsr = emptyList(),
                reiseTilSamlingTso = emptyList(),
                reiseTilSamlingTsr = emptyList(),
            )
    }
}
