package no.nav.tilleggsstonader.sak.behandling.vent

import java.time.LocalDate
import java.time.LocalDateTime

data class SettPåVentDto(
    val årsaker: List<ÅrsakSettPåVent>,
    val frist: LocalDate,
    val kommentar: String?,
    val beholdOppgave: Boolean = false,
) {
    fun tilDomene() =
        SettBehandlingPåVent(
            årsaker = årsaker,
            frist = frist,
            kommentar = kommentar,
            oppgaveMetadata = SettBehandlingPåVentOppgaveMetadata.OppdaterOppgave(beholdOppgave = beholdOppgave),
        )
}

data class OppdaterSettPåVentDto(
    val årsaker: List<ÅrsakSettPåVent>,
    val frist: LocalDate,
    val kommentar: String?,
    val oppgaveVersjon: Int,
    val beholdOppgave: Boolean = false,
)

data class StatusPåVentDto(
    val årsaker: List<ÅrsakSettPåVent>,
    val kommentar: String?,
    val datoSattPåVent: LocalDate,
    val opprettetAv: String,
    val endretAv: String?,
    val endretTid: LocalDateTime?,
    val frist: LocalDate?,
)

data class TaAvVentDto(
    val skalTilordnesRessurs: Boolean,
    val kommentar: String?,
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
