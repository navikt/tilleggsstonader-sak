package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status

import java.time.LocalDate

data class UtbetalingStatusRecord(
    val status: UtbetalingStatus,
    val detaljer: UtbetalingStatusDetaljer,
)

data class UtbetalingStatusDetaljer(
    val ytelse: String, // TILLEGGSSTØNADER, DAGPENGER, etc.
    val linjer: List<UtbetalingLinje>,
)

data class UtbetalingLinje(
    val behandlingId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val vedtakssats: UInt,
    val beløp: UInt,
    val klassekode: String,
)

enum class UtbetalingStatus {
    OK,
    FEILET,
    MOTTATT,
    HOS_OPPDRAG,
}
