package no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto

import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.Behandlingstype
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.MappeDto
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.Sorteringsfelt
import no.nav.tilleggsstonader.kontrakter.oppgave.Sorteringsrekkefølge
import java.time.LocalDate

/**
 * @param oppgaverPåVent: Hent oppgaver kun oppgaver på vent hvis true, og kun oppgaver som ikke er på vent hvis false
 */
data class FinnOppgaveRequestDto(
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
    val oppgavetype: String? = null,
    val enhet: String? = null,
    val saksbehandler: String? = null,
    val journalpostId: String? = null,
    val tilordnetRessurs: String? = null,
    val tildeltRessurs: Boolean? = null,
    val opprettetFom: LocalDate? = null,
    val opprettetTom: LocalDate? = null,
    val fristFom: LocalDate? = null,
    val fristTom: LocalDate? = null,
    val ident: String?,
    val limit: Long = 150,
    val offset: Long = 0,
    val orderBy: Sorteringsfelt = Sorteringsfelt.OPPRETTET_TIDSPUNKT,
    val order: Sorteringsrekkefølge = Sorteringsrekkefølge.ASC,
    val oppgaverPåVent: Boolean = false,
) {
    fun tilFinnOppgaveRequest(
        aktørid: String? = null,
        klarmappe: MappeDto,
        ventemappe: MappeDto,
    ): FinnOppgaveRequest =
        FinnOppgaveRequest(
            tema = Tema.TSO,
            behandlingstema =
                if (this.behandlingstema != null) {
                    Behandlingstema.entries.find { it.value == this.behandlingstema }
                } else {
                    null
                },
            behandlingstype =
                if (this.behandlingstype != null) {
                    Behandlingstype.entries.find { it.value == this.behandlingstype }
                } else {
                    null
                },
            oppgavetype =
                if (this.oppgavetype != null) {
                    Oppgavetype.entries.find { it.value == this.oppgavetype }
                } else {
                    null
                },
            enhet = this.enhet,
            erUtenMappe = false,
            saksbehandler = this.saksbehandler,
            aktørId = aktørid,
            journalpostId = this.journalpostId,
            tildeltRessurs = this.tildeltRessurs,
            tilordnetRessurs = this.tilordnetRessurs,
            opprettetFomTidspunkt = this.opprettetFom?.atStartOfDay(),
            opprettetTomTidspunkt = this.opprettetTom?.plusDays(1)?.atStartOfDay(),
            fristFomDato = this.fristFom,
            fristTomDato = this.fristTom,
            aktivFomDato = null,
            aktivTomDato = null,
            mappeId = if (this.oppgaverPåVent) ventemappe.id else klarmappe.id,
            limit = this.limit,
            offset = this.offset,
            sorteringsrekkefolge = order,
            sorteringsfelt = orderBy,
        )
}
