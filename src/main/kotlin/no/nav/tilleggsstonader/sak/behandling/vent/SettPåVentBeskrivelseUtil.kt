package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.util.medGosysTid
import no.nav.tilleggsstonader.sak.util.norskFormat
import java.time.LocalDate
import java.time.LocalDateTime

object SettPåVentBeskrivelseUtil {

    fun settPåVent(
        oppgave: Oppgave,
        dto: SettPåVentDto,
        tidspunkt: LocalDateTime = LocalDateTime.now(),
    ): String {
        return utledBeskrivelsePrefix(tidspunkt) +
            utledOppgavefristBeskrivelse(oppgave, dto.frist).påNyRadEllerTomString() +
            dto.kommentar.påNyRadEllerTomString() +
            nåværendeBeskrivelse(oppgave)
    }

    fun oppdaterSettPåVent(
        oppgave: Oppgave,
        dto: OppdaterSettPåVentDto,
        tidspunkt: LocalDateTime = LocalDateTime.now(),
    ): String {
        return utledBeskrivelsePrefix(tidspunkt) +
            utledOppgavefristBeskrivelse(oppgave, dto.frist).påNyRadEllerTomString() +
            dto.kommentar.påNyRadEllerTomString() +
            nåværendeBeskrivelse(oppgave)
    }

    fun taAvVent(oppgave: Oppgave, tidspunkt: LocalDateTime = LocalDateTime.now()): String {
        return utledBeskrivelsePrefix(tidspunkt) + "\nTatt av vent" + nåværendeBeskrivelse(oppgave)
    }

    private fun String?.påNyRadEllerTomString(): String = this?.trim()?.takeIf { it.isNotBlank() }?.let { "\n$it" } ?: ""

    private fun nåværendeBeskrivelse(oppgave: Oppgave): String {
        return if (oppgave.beskrivelse.isNullOrBlank()) {
            ""
        } else {
            "\n\n${oppgave.beskrivelse}"
        }
    }

    private fun utledBeskrivelsePrefix(tidspunkt: LocalDateTime): String {
        val innloggetSaksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
        val saksbehandlerNavn = SikkerhetContext.hentSaksbehandlerNavn(strict = false)

        return "--- ${tidspunkt.medGosysTid()} $saksbehandlerNavn ($innloggetSaksbehandlerIdent) ---"
    }

    private fun utledOppgavefristBeskrivelse(
        oppgave: Oppgave,
        frist: LocalDate,
    ): String {
        val eksisterendeFrist = oppgave.fristFerdigstillelse?.norskFormat() ?: "<ingen>"
        val fristNorskFormat = frist.norskFormat()
        return if (eksisterendeFrist == fristNorskFormat) "" else "Oppgave endret frist fra $eksisterendeFrist til $fristNorskFormat"
    }
}
