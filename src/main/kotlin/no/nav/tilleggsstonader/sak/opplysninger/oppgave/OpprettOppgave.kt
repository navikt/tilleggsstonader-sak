package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.felles.tilBehandlingstema
import no.nav.tilleggsstonader.kontrakter.oppgave.IdentGruppe
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveIdentV2
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveUtil.lagFristForOppgave
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveUtil.utledBehandlesAvApplikasjon
import no.nav.tilleggsstonader.sak.util.medGosysTid
import java.time.LocalDate

data class OpprettOppgave(
    val oppgavetype: Oppgavetype,
    val beskrivelse: String? = null,
    val tilordnetNavIdent: String? = null,
    val enhetsnummer: String? = null,
    val prioritet: OppgavePrioritet = OppgavePrioritet.NORM,
    val fristFerdigstillelse: LocalDate? = null,
    val journalpostId: String? = null,
)

fun tilOpprettOppgaveRequest(
    oppgave: OpprettOppgave,
    personIdent: String,
    stønadstype: Stønadstype,
    enhetsnummer: String? = null,
    mappeId: Long?,
): OpprettOppgaveRequest =
    OpprettOppgaveRequest(
        ident = OppgaveIdentV2(ident = personIdent, gruppe = IdentGruppe.FOLKEREGISTERIDENT),
        tema = Tema.TSO,
        journalpostId = oppgave.journalpostId,
        oppgavetype = oppgave.oppgavetype,
        fristFerdigstillelse = oppgave.fristFerdigstillelse ?: lagFristForOppgave(osloNow()),
        beskrivelse = lagOppgaveTekst(oppgave.beskrivelse),
        enhetsnummer = enhetsnummer,
        behandlingstema = stønadstype.tilBehandlingstema().value,
        tilordnetRessurs = oppgave.tilordnetNavIdent,
        mappeId = mappeId,
        prioritet = oppgave.prioritet,
        behandlesAvApplikasjon = utledBehandlesAvApplikasjon(oppgave.oppgavetype),
    )

private fun lagOppgaveTekst(beskrivelse: String? = null): String {
    val tidspunkt = osloNow().medGosysTid()
    val prefix = "----- Opprettet av tilleggsstonader-sak $tidspunkt ---"
    val beskrivelseMedNewLine = beskrivelse?.let { "\n$it" } ?: ""
    return prefix + beskrivelseMedNewLine
}
