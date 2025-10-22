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
    val sakId: String,
    val behandlingId: String,
    val personident: String,
    val saksbehandler: String,
    val beslutter: String,
    val vedtakstidspunkt: LocalDateTime,
    val periodetype: PeriodetypeUtbetaling,
    // kan ikke ha flere stønader i en og samme kjede i v3 (koden som splittet opp dette er ikke i bruk - umulig å vedlikeholde)
    val stønad: StønadUtbetaling,
    val perioder: List<PerioderUtbetaling>,
)

enum class StønadUtbetaling {
    // "kontonummer". Skal den endres må den gamle opphøres først. (I prakis en tom liste med perioder)
    DAGLIG_REISE_ENSLIG_FORSØRGER,
    DAGLIG_REISE_AAP,
    DAGLIG_REISE_ETTERLATTE,
}

/**
 * Alt 1: UTGÅR - Hver billett får sin egen engangsutbetaling, og vi sender med forrige iverksettingId og hele historikken hver gang *FUNKER IKKE PÅ KAFKA*
 *
 * Alt 2: perioden gjelder et helt år eller måned av gangen. Beløpet økes etter hvert som bruker skal ha flere billetter
 * Lista med måneder må være totalbildet (maks ett pr mnd)
 * Utbetalingsdatoen styres av "utbetalingsdato" i daglig jobb
 * Engangsutbetalinger blir utbetalt dagen etter
 * Skal vi bruke DAG (med fom=tom) eller EN_GANG (med fom=første, TOM=siste i mnd)
 * En utbetaling per fagsak
 * Hvis vi har ulike TypeAndel må vi sende i samme transaksjon men i forskjellige utbetalinger
 *
 * Alt 3: Ha én periode i lista per billett
 */
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
