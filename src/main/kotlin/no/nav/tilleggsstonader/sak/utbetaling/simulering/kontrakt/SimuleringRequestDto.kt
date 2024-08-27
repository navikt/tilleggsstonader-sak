package no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt

import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.ForrigeIverksettingDto
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.UtbetalingDto

data class SimuleringRequestDto(
    val sakId: String,
    val behandlingId: String,
    val personident: String,
    val saksbehandlerId: String,
    val utbetalinger: List<UtbetalingDto>,
    val forrigeIverksetting: ForrigeIverksettingDto? = null,
)
