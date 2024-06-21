package no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt

import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.ForrigeIverksettingDto
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.UtbetalingDto
import java.time.LocalDateTime

data class SimuleringRequestDto(
    val sakId: String,
    val behandlingId: String,
    val personident: String,
    val saksbehandler: String,
    val vedtakstidspunkt: LocalDateTime,
    val utbetalinger: List<UtbetalingDto>,
    val forrigeIverksetting: ForrigeIverksettingDto? = null,
)
