package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse

import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse

fun TilkjentYtelse.tilDto(): TilkjentYtelseDto {
    return TilkjentYtelseDto(
        behandlingId = this.behandlingId,
        andeler = this.andelerTilkjentYtelse.map { andel -> andel.tilDto() },
    )
}

fun AndelTilkjentYtelse.tilDto(): AndelTilkjentYtelseDto {
    return AndelTilkjentYtelseDto(
        beløp = this.beløp,
        periode = this.periode,
    )
}
