package no.nav.tilleggsstonader.sak.tilkjentytelse

import no.nav.tilleggsstonader.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.util.Månedsperiode
import java.time.LocalDate
import java.util.UUID

fun lagTilkjentYtelse(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    id: UUID = UUID.randomUUID(),
    behandlingId: UUID = UUID.randomUUID(),
    startdato: LocalDate = andelerTilkjentYtelse.minOfOrNull { it.stønadFom } ?: LocalDate.now(),
) =
    TilkjentYtelse(
        id = id,
        behandlingId = behandlingId,
        andelerTilkjentYtelse = andelerTilkjentYtelse,
        startdato = startdato,
    )

fun lagAndelTilkjentYtelse(
    beløp: Int,
    fraOgMed: LocalDate,
    tilOgMed: LocalDate,
    kildeBehandlingId: UUID = UUID.randomUUID(),
) =
    AndelTilkjentYtelse(
        beløp = beløp,
        periode = Månedsperiode(fraOgMed, tilOgMed),
        kildeBehandlingId = kildeBehandlingId,
    )
