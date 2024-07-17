package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import java.time.LocalDate
import java.time.LocalDateTime

data class SettPåVentDto(
    val årsaker: List<ÅrsakSettPåVent>,
    val frist: LocalDate,
    val kommentar: String?,
) {
    init {
        brukerfeilHvis(årsaker.any { it == ÅrsakSettPåVent.ANNET } && kommentar.isNullOrBlank()) {
            "Mangler påkrevd begrunnelse når man valgt Annet"
        }
        brukerfeilHvis((kommentar?.length ?: 0) > 1000) {
            "Kan ikke sende inn kommentar med over 1000 tegn"
        }
    }
}

data class OppdaterSettPåVentDto(
    val årsaker: List<ÅrsakSettPåVent>,
    val frist: LocalDate,
    val kommentar: String?,
    val oppgaveVersjon: Int,
)

data class StatusPåVentDto(
    val årsaker: List<ÅrsakSettPåVent>,
    val kommentar: String?,
    val datoSattPåVent: LocalDate,
    val opprettetAv: String,
    val endretAv: String?,
    val endretTid: LocalDateTime?,
    val frist: LocalDate?,
    val oppgaveVersjon: Int,
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
