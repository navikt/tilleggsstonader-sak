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
    MOTTATT, // sendes når Helved har lest meldingen vår. Vil denne sendes så fort at vi ikke rekker å comitte til databasen?
    FEILET, // sendes ved valideringfeil hos Helved eller hvis noe feiler mot Oppdrag
    HOS_OPPDRAG, // sendes når oppdrag har mottatt utbetalingen. Kan ligge i denne statusen en stund hvis Oppdrag er stengt.
    OK, // kvittert ut hos Oppdrag
}
