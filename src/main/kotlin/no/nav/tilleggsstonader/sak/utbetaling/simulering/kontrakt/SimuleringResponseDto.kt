package no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt

import java.time.LocalDate

data class SimuleringResponseDto(
    val oppsummeringer: List<OppsummeringForPeriode>,
    val detaljer: SimuleringDetaljer,
)

data class OppsummeringForPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val tidligereUtbetalt: Int,
    val nyUtbetaling: Int,
    val totalEtterbetaling: Int,
    val totalFeilutbetaling: Int,
)

data class SimuleringDetaljer(
    val gjelderId: String,
    val datoBeregnet: LocalDate,
    val totalBeløp: Int,
    val perioder: List<Periode>,
)

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
    val posteringer: List<Postering>,
)

data class Postering(
    val fagområde: Fagområde,
    val sakId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
    val type: PosteringType,
    val klassekode: String,
)

enum class Fagområde(
    val kode: String,
) {
    TILLEGGSSTØNADER("TILLST"),
    TILLEGGSSTØNADER_ARENA("TSTARENA"),
    TILLEGGSSTØNADER_ARENA_MANUELL_POSTERING("MTSTAREN"),
}
