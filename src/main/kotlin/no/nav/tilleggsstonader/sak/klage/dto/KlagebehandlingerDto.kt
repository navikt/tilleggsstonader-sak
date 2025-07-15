package no.nav.tilleggsstonader.sak.klage.dto

import no.nav.tilleggsstonader.kontrakter.klage.KlagebehandlingDto

data class KlagebehandlingerDto(
    val tilsynBarn: List<KlagebehandlingDto>,
    val læremidler: List<KlagebehandlingDto>,
    val boutgifter: List<KlagebehandlingDto>,
    val dagligReiseTSO: List<KlagebehandlingDto>,
    val dagligReiseTSR: List<KlagebehandlingDto>,
) {
    companion object {
        fun empty() =
            KlagebehandlingerDto(
                tilsynBarn = emptyList(),
                læremidler = emptyList(),
                boutgifter = emptyList(),
                dagligReiseTSO = emptyList(),
                dagligReiseTSR = emptyList(),
            )
    }
}
