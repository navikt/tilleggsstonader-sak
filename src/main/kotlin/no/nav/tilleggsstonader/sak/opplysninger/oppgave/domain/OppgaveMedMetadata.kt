package no.nav.tilleggsstonader.sak.opplysninger.oppgave.domain

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveBehandlingMetadata
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.Oppgavestatus

data class OppgaveMedMetadata(
    val oppgave: Oppgave,
    val metadata: OppgaveMetadata?,
)

data class OppgaveMetadata(
    val navn: String?,
    val behandlingMetadata: OppgaveBehandlingMetadata?,
)

data class OppdatertOppgaveHendelse(
    val gsakOppgaveId: Long,
    val tilordnetSaksbehandler: String?,
    val status: Oppgavestatus,
    val tildeltEnhetsnummer: String,
    val enhetsmappeId: Long?,
)
