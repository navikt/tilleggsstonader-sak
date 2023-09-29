package no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt

import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.kontrakt.TilkjentYtelseDto
import java.time.LocalDate
import java.util.UUID

data class SimuleringDto(
    val nyTilkjentYtelseMedMetaData: TilkjentYtelseMedMetadata,
    val forrigeBehandlingId: UUID?,
)

data class TilkjentYtelseMedMetadata(
    val tilkjentYtelse: TilkjentYtelseDto,
    val saksbehandlerId: String,
    val eksternBehandlingId: Long,
    val stønadstype: Stønadstype,
    val eksternFagsakId: Long,
    val behandlingId: UUID,
    val vedtaksdato: LocalDate,
)
