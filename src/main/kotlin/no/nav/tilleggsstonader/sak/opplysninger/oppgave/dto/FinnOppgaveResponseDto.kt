package no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveIdentV2
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.kontrakter.oppgave.StatusEnum
import java.time.LocalDate
import java.util.Optional

data class FinnOppgaveResponseDto(
    val antallTreffTotalt: Long,
    val oppgaver: List<OppgaveDto>,
)

/**
 * Kopi av [no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave] med ekstrafelt
 * * Navn
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OppgaveDto(
    val id: Long,
    val versjon: Int,
    val identer: List<OppgaveIdentV2>?,
    val tildeltEnhetsnr: String?,
    val endretAvEnhetsnr: String?,
    val opprettetAvEnhetsnr: String?,
    val journalpostId: String?,
    val journalpostkilde: String?,
    val behandlesAvApplikasjon: String?,
    val saksreferanse: String?,
    val bnr: String?,
    val samhandlernr: String?,
    val aktoerId: String?,
    val personident: String?,
    val orgnr: String?,
    val tilordnetRessurs: String?,
    val beskrivelse: String?,
    val temagruppe: String?,
    val tema: Tema?,
    val behandlingstema: String?,
    val oppgavetype: String?,
    val behandlingstype: String?,
    val mappeId: Optional<Long>?,
    val fristFerdigstillelse: LocalDate?,
    val aktivDato: LocalDate?,
    val opprettetTidspunkt: String?,
    val opprettetAv: String?,
    val endretAv: String?,
    val ferdigstiltTidspunkt: String?,
    val endretTidspunkt: String?,
    val prioritet: OppgavePrioritet?,
    val status: StatusEnum?,

    /**
     * Ekstra felter
     */
    val navn: String?,
) {
    constructor(
        oppgave: Oppgave,
        navn: String?,
    ) : this(
        id = oppgave.id,
        versjon = oppgave.versjon,
        identer = oppgave.identer,
        tildeltEnhetsnr = oppgave.tildeltEnhetsnr,
        endretAvEnhetsnr = oppgave.endretAvEnhetsnr,
        opprettetAvEnhetsnr = oppgave.opprettetAvEnhetsnr,
        journalpostId = oppgave.journalpostId,
        journalpostkilde = oppgave.journalpostkilde,
        behandlesAvApplikasjon = oppgave.behandlesAvApplikasjon,
        saksreferanse = oppgave.saksreferanse,
        bnr = oppgave.bnr,
        samhandlernr = oppgave.samhandlernr,
        aktoerId = oppgave.aktoerId,
        personident = oppgave.personident,
        orgnr = oppgave.orgnr,
        tilordnetRessurs = oppgave.tilordnetRessurs,
        beskrivelse = oppgave.beskrivelse,
        temagruppe = oppgave.temagruppe,
        tema = oppgave.tema,
        behandlingstema = oppgave.behandlingstema,
        oppgavetype = oppgave.oppgavetype,
        behandlingstype = oppgave.behandlingstype,
        mappeId = oppgave.mappeId,
        fristFerdigstillelse = oppgave.fristFerdigstillelse,
        aktivDato = oppgave.aktivDato,
        opprettetTidspunkt = oppgave.opprettetTidspunkt,
        opprettetAv = oppgave.opprettetAv,
        endretAv = oppgave.endretAv,
        ferdigstiltTidspunkt = oppgave.ferdigstiltTidspunkt,
        endretTidspunkt = oppgave.endretTidspunkt,
        prioritet = oppgave.prioritet,
        status = oppgave.status,

        navn = navn,
    )
}
