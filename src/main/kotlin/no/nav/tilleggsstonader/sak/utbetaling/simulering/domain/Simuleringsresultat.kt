package no.nav.tilleggsstonader.sak.utbetaling.simulering.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.PosteringType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

/**
 * @property ingenEndringIUtbetaling: Hvis man simulerer med identiske utbetalinger som tidligere vil utsjekk svare med 204 NO CONTENT,
 * fordi det ikke er noen utbetalinger å simulere. Vi setter denne til true for å indikere at det ikke er noen endring i utbetaling.
 */
@Table
data class Simuleringsresultat(
    @Id
    val behandlingId: BehandlingId,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    val data: SimuleringJson?,
    val ingenEndringIUtbetaling: Boolean = false,
)

data class SimuleringJson(
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

enum class Fagområde(val kode: String) {
    TILLEGGSSTØNADER("TILLST"),
    TILLEGGSSTØNADER_ARENA("TSTARENA"),
    TILLEGGSSTØNADER_ARENA_MANUELL_POSTERING("MTSTAREN"),
}
