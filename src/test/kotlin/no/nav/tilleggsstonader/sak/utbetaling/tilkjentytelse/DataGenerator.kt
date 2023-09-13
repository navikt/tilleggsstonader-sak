package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse

import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import java.time.LocalDate
import java.util.Random
import java.util.UUID

object DataGenerator {

    private fun flereTilfeldigeAndelerTilkjentYtelse(antall: Int, behandlingId: UUID): List<AndelTilkjentYtelse> =
        (1..antall).map { tilfeldigAndelTilkjentYtelse(behandlingId = behandlingId) }.toList()

    private fun tilfeldigAndelTilkjentYtelse(
        beløp: Int = Random().nextInt(20_000) + 1,
        stønadFom: LocalDate = LocalDate.now(),
        stønadTom: LocalDate = LocalDate.now(),
        behandlingId: UUID,
    ) =
        AndelTilkjentYtelse(
            beløp = beløp,
            stønadFom = stønadFom,
            stønadTom = stønadTom,
            kildeBehandlingId = behandlingId,
        )

    fun tilfeldigTilkjentYtelse(
        behandling: Behandling = behandling(fagsak()),
        antallAndelerTilkjentYtelse: Int = 1,
    ): TilkjentYtelse {
        val andelerTilkjentYtelse = flereTilfeldigeAndelerTilkjentYtelse(antallAndelerTilkjentYtelse, behandling.id)
        return TilkjentYtelse(
            behandlingId = behandling.id,
            startdato = andelerTilkjentYtelse.minOf { it.stønadFom },
            andelerTilkjentYtelse = andelerTilkjentYtelse,
        )
    }
}
