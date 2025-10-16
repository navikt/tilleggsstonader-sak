package no.nav.tilleggsstonader.sak.utbetaling.utsjekk

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * [uid] er en unik ID som konsumenter av Utsjekk har ansvar for å lage og holde styr
 * på i egen løsning. Denne brukes til å unikt identifisere utbetalingen og kan brukes
 * når man evt. ønsker å gjøre endringer eller opphør på en utbetaling.
 */
data class UtbetalingRecord(
    val dryrun: Boolean = false,
    val id: UUID = UUID.randomUUID(),
    val brukFagområdeTillst: Boolean = false,
    val sakId: String,
    val behandlingId: BehandlingId,
    val personident: String,
    val stønad: StønadUtbetaling,
    val saksbehandler: String,
    val beslutter: String,
    val vedtakstidspunkt: LocalDateTime,
    val periodetype: PeriodetypeUtbetaling,
    val perioder: List<PerioderUtbetaling>,
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
)

enum class PeriodetypeUtbetaling {
    UKEDAG,
    MND,
    EN_GANG,
}
