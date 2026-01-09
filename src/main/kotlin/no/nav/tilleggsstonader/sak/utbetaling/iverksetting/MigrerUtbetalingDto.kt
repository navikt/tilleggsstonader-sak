package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import java.util.UUID

data class MigrerUtbetalingDto(
    val sakId: String,
    val behandlingId: String,
    val iverksettingId: String?,
    val meldeperiode: String?,
    val uidToStønad: Pair<UUID, StønadstypeIverksetting>, // utbetalingId -> StønadstypeIverksetting
)
