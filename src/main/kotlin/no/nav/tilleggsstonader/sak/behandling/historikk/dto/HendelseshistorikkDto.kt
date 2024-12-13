package no.nav.tilleggsstonader.sak.behandling.historikk.dto

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import java.time.LocalDateTime

data class HendelseshistorikkDto(
    val behandlingId: BehandlingId,
    var hendelse: Hendelse,
    val endretAvNavn: String,
    val endretTid: LocalDateTime,
    val metadata: Map<String, Any>? = null,
)

enum class Hendelse {
    OPPRETTET,
    SATT_PÅ_VENT,
    TATT_AV_VENT,
    SENDT_TIL_BESLUTTER,
    VEDTAK_GODKJENT,
    VEDTAK_UNDERKJENT,
    VEDTAK_IVERKSATT,
    VEDTAK_AVSLÅTT,
    HENLAGT,
    UKJENT,
    ANGRE_SEND_TIL_BESLUTTER,
    ;

    fun sattEllerTattAvVent() = this == SATT_PÅ_VENT || this == TATT_AV_VENT
}
