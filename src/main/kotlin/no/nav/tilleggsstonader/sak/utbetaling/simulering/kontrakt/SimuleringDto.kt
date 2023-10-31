package no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt

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
    val stønadstype: no.nav.tilleggsstonader.kontrakter.felles.Stønadstype,
    val eksternFagsakId: Long,
    val behandlingId: UUID,
    val vedtaksdato: LocalDate,
)
