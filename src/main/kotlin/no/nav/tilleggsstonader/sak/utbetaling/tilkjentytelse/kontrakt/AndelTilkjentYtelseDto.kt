package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.kontrakt

import no.nav.tilleggsstonader.sak.util.Månedsperiode
import java.time.YearMonth
import java.util.UUID

data class TilkjentYtelseDto(
    val andelerTilkjentYtelse: List<AndelTilkjentYtelseDto>,
    val startmåned: YearMonth,
)

data class AndelTilkjentYtelseDto(
    val beløp: Int,
    val periode: Månedsperiode,
    val kildeBehandlingId: UUID,
)
