package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Kopie fra dp-kontrakt, ønsker ikke å dra inn den avhengigheten
 * https://github.com/navikt/dp-kontrakter/blob/main/iverksett/src/main/kotlin/no/nav/dagpenger/kontrakter/iverksett/IverksettTilleggsst%C3%B8naderDto.kt
 */
data class IverksettDto(
    val sakId: String,
    val behandlingId: UUID,
    val iverksettingId: UUID,
    val personident: String,
    val vedtak: VedtaksdetaljerDto,
    val forrigeIverksetting: ForrigeIverksettingDto? = null,
)

data class VedtaksdetaljerDto(
    val vedtakstidspunkt: LocalDateTime,
    val saksbehandlerId: String,
    val beslutterId: String,
    val utbetalinger: List<UtbetalingDto>,
)

data class ForrigeIverksettingDto(
    val behandlingId: UUID,
    val iverksettingId: UUID,
)

data class UtbetalingDto(
    val beløp: Int,
    val satstype: SatstypeIverksetting,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate,
    val stønadstype: StønadstypeIverksetting,
    val brukersNavKontor: BrukersNavKontor? = null, // denne gjelder kun tiltak?
)

enum class SatstypeIverksetting {
    DAGLIG,
    MÅNEDLIG,
    ENGANGS,
}

data class BrukersNavKontor(
    val enhet: String,
    val gjelderFom: LocalDate,
)

/**
 * Hvis det legges til nye typer her.
 * Husk å se om man trenger å legge til navkontor, og at det iverksettes på riktig måte i dp-iverksett
 * Eks for reise kan det finnes behov for å sende kontor på på utbetalingperiode-nivå
 */
enum class StønadstypeIverksetting {
    TILSYN_BARN_ENSLIG_FORSØRGER,
    TILSYN_BARN_AAP,
    TILSYN_BARN_ETTERLATTE,
}

enum class IverksettStatus {
    SENDT_TIL_OPPDRAG,
    FEILET_MOT_OPPDRAG,
    OK,
    IKKE_PAABEGYNT,
}
