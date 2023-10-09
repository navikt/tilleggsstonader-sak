package no.nav.tilleggsstonader.sak.vedtak.dto

import java.time.LocalDateTime
import java.util.Collections.emptyList

data class BeslutteVedtakDto(
    val godkjent: Boolean,
    val begrunnelse: String? = null,
    val årsak: List<Årsak> = emptyList(),
)

data class StatusTotrinnskontrollDto(
    val status: TotrinnkontrollStatus,
    val totrinnskontroll: TotrinnskontrollDto? = null,
)

data class TotrinnskontrollDto(
    val opprettetAv: String,
    val opprettetTid: LocalDateTime,
    val godkjent: Boolean? = null,
    val begrunnelse: String? = null,
    val årsakerUnderkjent: List<Årsak> = emptyList(),
)

enum class TotrinnkontrollStatus {
    TOTRINNSKONTROLL_UNDERKJENT,
    KAN_FATTE_VEDTAK,
    IKKE_AUTORISERT,
    UAKTUELT,
}

enum class Årsak {
    TIDLIGERE_VEDTAKSPERIODER,
    INNGANGSVILKÅR_FORUTGÅENDE_MEDLEMSKAP_OPPHOLD,
    INNGANGSVILKÅR_ALENEOMSORG,
    AKTIVITET,
    VEDTAK_OG_BEREGNING,
    VEDTAKSBREV,
    RETUR_ETTER_ØNSKE_FRA_SAKSBEHANDLER,
}
