package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.UtbetalingId

data class MigrerUtbetalingDto(
    val sakId: String,
    val behandlingId: String,
    val iverksettingId: String?,
    val meldeperiode: String?,
    val uidToStønad: Pair<UtbetalingId, StønadstypeIverksetting>,
)
