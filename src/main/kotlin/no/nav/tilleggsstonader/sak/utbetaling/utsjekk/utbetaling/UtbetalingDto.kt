package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import com.fasterxml.jackson.annotation.JsonUnwrapped
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

sealed interface UtbetalingDto

data class SimuleringDto(
    @JsonUnwrapped val utbetalingsgrunnlag: UtbetalingGrunnlagDto,
    val vedtakstidspunkt: LocalDateTime = LocalDateTime.now(),
) : UtbetalingDto {
    val dryrun: Boolean = true
}

class IverksettingDto(
    @JsonUnwrapped val utbetalingsgrunnlag: UtbetalingGrunnlagDto,
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

    DAGLIG_REISE_TILTAK_ARBEIDSFORBEREDENDE,
    DAGLIG_REISE_TILTAK_ARBEIDSRETTET_REHAB,
    DAGLIG_REISE_TILTAK_ARBEIDSTRENING,
    DAGLIG_REISE_TILTAK_AVKLARING,
    DAGLIG_REISE_TILTAK_DIGITAL_JOBBKLUBB,
    DAGLIG_REISE_TILTAK_ENKELTPLASS_AMO,
    DAGLIG_REISE_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD,
    DAGLIG_REISE_TILTAK_FORSØK_OPPLÆRINGSTILTAK_LENGER_VARIGHET,
    DAGLIG_REISE_TILTAK_GRUPPE_AMO,
    DAGLIG_REISE_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD,
    DAGLIG_REISE_TILTAK_HØYERE_UTDANNING,
    DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE,
    DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE_UNG,
    DAGLIG_REISE_TILTAK_JOBBKLUBB,
    DAGLIG_REISE_TILTAK_OPPFØLGING,
    DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_NAV,
    DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_OPPLÆRING,
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
