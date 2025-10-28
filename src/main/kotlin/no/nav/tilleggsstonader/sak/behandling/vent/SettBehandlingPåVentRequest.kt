package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import java.time.LocalDate

data class SettBehandlingPåVentRequest(
    val årsaker: List<ÅrsakSettPåVent>,
    val frist: LocalDate,
    val kommentar: String?,
    val oppdaterOppgave: Boolean = true,
    val beholdOppgave: Boolean = false,
) {
    init {
        brukerfeilHvis(årsaker.any { it == ÅrsakSettPåVent.ANNET } && kommentar.isNullOrBlank()) {
            "Kommentar er påkrevd ved valg av årsak 'Annet'"
        }
        brukerfeilHvis((kommentar?.length ?: 0) > 1000) {
            "Kommentar kan maks være 1000 tegn"
        }
        feilHvis(!oppdaterOppgave && beholdOppgave) {
            "Kan ikke beholde oppgave når oppdaterOppgave er $oppdaterOppgave"
        }
    }
}
