package no.nav.tilleggsstonader.sak.behandling.vent

import java.time.LocalDate

data class SettPåVentDto(
    val årsaker: List<ÅrsakSettPåVent>,
    val frist: LocalDate,
    val kommentar: String?,
)

data class OppdaterSettPåVentDto(
    val årsaker: List<ÅrsakSettPåVent>,
    val frist: LocalDate,
    val kommentar: String?,
    val oppgaveVersjon: Int,
)

data class StatusPåVentDto(
    val årsaker: List<ÅrsakSettPåVent>,
    val frist: LocalDate?,
    val oppgaveBeskrivelse: String?,
    val oppgaveVersjon: Int,
)

enum class ÅrsakSettPåVent {
    DOKUMENTASJON_FRA_BRUKER,
    REGISTRERING_AV_TILTAK,
    VURDERING_AV_NEDSATT_ARBEIDSEVNE,
    ADRESSE_TIL_TILTAKSARRANGØR,
    ANTALL_DAGER_PÅ_TILTAK,
    RETTIGHET_TIL_OVERGANGSSTØNAD,
    REGISTRERING_AV_UTDANNING,
    ANNET,
}
