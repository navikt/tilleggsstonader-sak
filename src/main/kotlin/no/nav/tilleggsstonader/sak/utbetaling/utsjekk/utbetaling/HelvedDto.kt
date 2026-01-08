package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private sealed interface HelvedDto {
    val sakId: String
    val behandlingId: String
    val personident: String
    val periodetype: PeriodetypeUtbetaling
    val vedtakstidspunkt: LocalDateTime
    val utbetalinger: List<UtbetalingDto>
    val dryrun: Boolean
}

data class SimuleringDto(
    override val sakId: String,
    override val behandlingId: String,
    override val personident: String,
    override val periodetype: PeriodetypeUtbetaling,
    override val vedtakstidspunkt: LocalDateTime = LocalDateTime.now(),
    override val utbetalinger: List<UtbetalingDto>,
) : HelvedDto {
    override val dryrun: Boolean = true
}

class IverksettingDto(
    override val sakId: String,
    override val behandlingId: String,
    override val personident: String,
    override val periodetype: PeriodetypeUtbetaling,
    override val vedtakstidspunkt: LocalDateTime,
    override val utbetalinger: List<UtbetalingDto>,
    val saksbehandler: String,
    val beslutter: String,
) : HelvedDto {
    override val dryrun: Boolean = false
}

/**
 * [id] er en UUID som konsumenter av Utsjekk har ansvar for å lage og holde styr
 * på i egen løsning. Denne brukes til å unikt identifisere utbetalingen og kan brukes
 * når man evt. ønsker å gjøre endringer eller opphør på en utbetaling.
 *
 */
data class UtbetalingDto(
    val id: UUID, // utbetalingId
    val stønad: StønadUtbetaling,
    val perioder: List<UtbetalingPeriodeDto>,
    val brukFagområdeTillst: Boolean,
)

data class UtbetalingPeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: UInt,
)

enum class StønadUtbetaling {
    // "kontonummer". Skal den endres må den gamle opphøres først. (I prakis en tom liste med perioder)
    DAGLIG_REISE_ENSLIG_FORSØRGER,
    DAGLIG_REISE_AAP,
    DAGLIG_REISE_ETTERLATTE,

    LÆREMIDLER_ENSLIG_FORSØRGER,
    LÆREMIDLER_AAP,
    LÆREMIDLER_ETTERLATTE,

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

enum class PeriodetypeUtbetaling {
    UKEDAG,
    MND,
    EN_GANG,
}
