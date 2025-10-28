package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import com.fasterxml.jackson.annotation.JsonUnwrapped
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

sealed interface UtbetalingDto

data class SimuleringDto(
    @JsonUnwrapped val grunnlag: UtbetalingGrunnlagDto,
) : UtbetalingDto {
    val dryrun: Boolean = true
    val vedtakstidspunkt: LocalDateTime = LocalDateTime.now()
}

class IverksettingDto(
    @JsonUnwrapped val grunnlag: UtbetalingGrunnlagDto,
    val saksbehandler: String,
    val beslutter: String,
    val vedtakstidspunkt: LocalDateTime,
) : UtbetalingDto {
    val dryrun: Boolean = false
}

/**
 * [id] er en UUID som konsumenter av Utsjekk har ansvar for å lage og holde styr
 * på i egen løsning. Denne brukes til å unikt identifisere utbetalingen og kan brukes
 * når man evt. ønsker å gjøre endringer eller opphør på en utbetaling.
 *
 */
data class UtbetalingGrunnlagDto(
    val id: UUID,
    val sakId: String,
    val behandlingId: String,
    val personident: String,
    val periodetype: PeriodetypeUtbetaling,
    val stønad: StønadUtbetaling,
    val perioder: List<PerioderUtbetaling>,
    val brukFagområdeTillst: Boolean = false,
)

enum class StønadUtbetaling {
    // "kontonummer". Skal den endres må den gamle opphøres først. (I prakis en tom liste med perioder)
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
