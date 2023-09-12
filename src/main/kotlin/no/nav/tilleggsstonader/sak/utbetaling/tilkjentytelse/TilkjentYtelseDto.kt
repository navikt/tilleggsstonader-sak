package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse

import no.nav.tilleggsstonader.sak.util.Månedsperiode
import java.util.UUID

data class TilkjentYtelseDto(
    val behandlingId: UUID,
    val andeler: List<AndelTilkjentYtelseDto>,
)

data class AndelTilkjentYtelseDto(
    val beløp: Int,
    val periode: Månedsperiode,
)
