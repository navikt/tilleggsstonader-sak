package no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto

import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.Sorteringsfelt
import no.nav.tilleggsstonader.kontrakter.oppgave.Sorteringsrekkefølge
import java.time.LocalDate

data class FinnOppgaveRequestDto(
    val behandlingstema: String? = null,
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
    val enhetsmappe: Long? = null,
    val mappeId: Long? = null,
    val erUtenMappe: Boolean? = null,
    val ident: String?,
    val limit: Long = 150, // TODO slett når frontend implementert limit og offset
    val offset: Long = 0, // TODO slett når frontend implementert limit og offset
    val orderBy: Sorteringsfelt = Sorteringsfelt.OPPRETTET_TIDSPUNKT, // TODO slett når frontend implementert
    val order: Sorteringsrekkefølge = Sorteringsrekkefølge.ASC, // TODO slett når frontend implementert
) {

    fun tilFinnOppgaveRequest(aktørid: String? = null): FinnOppgaveRequest =
        FinnOppgaveRequest(
            tema = Tema.TSO,
            behandlingstema = if (this.behandlingstema != null) {
                Behandlingstema.entries.find { it.value == this.behandlingstema }
            } else {
                null
            },
            oppgavetype = if (this.oppgavetype != null) {
                Oppgavetype.entries.find { it.value == this.oppgavetype }
            } else {
                null
            },
            enhet = this.enhet,
            erUtenMappe = this.erUtenMappe,
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
            mappeId = this.mappeId,
            limit = this.limit,
            offset = this.offset,
            sorteringsrekkefolge = order,
            sorteringsfelt = orderBy,
        )
}
