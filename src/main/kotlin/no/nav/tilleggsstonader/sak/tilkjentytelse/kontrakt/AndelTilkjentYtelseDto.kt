package no.nav.tilleggsstonader.sak.tilkjentytelse.kontrakt

import no.nav.tilleggsstonader.sak.util.Månedsperiode
import java.util.UUID

data class AndelTilkjentYtelseDto(
    val beløp: Int,
    val periode: Månedsperiode,
    val kildeBehandlingId: UUID,
)
