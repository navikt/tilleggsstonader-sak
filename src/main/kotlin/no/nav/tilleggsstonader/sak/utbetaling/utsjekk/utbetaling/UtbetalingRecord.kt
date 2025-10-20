package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * [id] er en UUID som konsumenter av Utsjekk har ansvar for å lage og holde styr
 * på i egen løsning. Denne brukes til å unikt identifisere utbetalingen og kan brukes
 * når man evt. ønsker å gjøre endringer eller opphør på en utbetaling.
 *
 */
data class UtbetalingRecord(
    val dryrun: Boolean = false,
    val brukFagområdeTillst: Boolean = false,
    val id: UUID,
    val forrigeUtbetaling: ForrigeUtbetaling?,
    val sakId: String,
    val behandlingId: String,
    val personident: String,
    val saksbehandler: String,
    val beslutter: String,
    val vedtakstidspunkt: LocalDateTime,
    val periodetype: PeriodetypeUtbetaling,
    val perioder: List<PerioderUtbetaling>,
)

data class ForrigeUtbetaling(
    val id: UUID,
    val behandlingId: String,
)

enum class StønadUtbetaling {
    DAGLIG_REISE_ENSLIG_FORSØRGER,
    DAGLIG_REISE_AAP,
    DAGLIG_REISE_ETTERLATTE,
}

data class PerioderUtbetaling(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: UInt,
    val stønad: StønadUtbetaling,
)

enum class PeriodetypeUtbetaling {
    UKEDAG,
    MND,
    EN_GANG,
}
