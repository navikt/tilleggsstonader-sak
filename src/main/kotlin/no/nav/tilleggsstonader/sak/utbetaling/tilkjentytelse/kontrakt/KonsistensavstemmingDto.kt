package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.kontrakt

import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import java.time.LocalDateTime
import java.util.UUID

data class KonsistensavstemmingDto(
    val stønadType: Stønadstype,
    val tilkjenteYtelser: List<KonsistensavstemmingTilkjentYtelseDto>,
    val avstemmingstidspunkt: LocalDateTime? = null,
)

data class KonsistensavstemmingTilkjentYtelseDto(
    val behandlingId: UUID,
    val eksternBehandlingId: Long,
    val eksternFagsakId: Long,
    val personIdent: String,
    val andelerTilkjentYtelse: List<AndelTilkjentYtelseDto>,
)
