package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import java.time.LocalDate

data class OpprettOppgave(
    val oppgavetype: Oppgavetype,
    val beskrivelse: String? = null,
    val tilordnetNavIdent: String? = null,
    val enhetsnummer: String? = null,
    val mappeId: Long? = null,
    val prioritet: OppgavePrioritet = OppgavePrioritet.NORM,
    val fristFerdigstillelse: LocalDate? = null,
    val journalpostId: String? = null,
)
