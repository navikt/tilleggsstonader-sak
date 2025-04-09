package no.nav.tilleggsstonader.sak.opplysninger.oppgave.domain

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveBehandlingMetadata

data class OppgaveMedMetadata(
    val oppgave: Oppgave,
    val metadata: OppgaveMetadata?,
)

data class OppgaveMetadata(
    val navn: String?,
    val behandlingMetadata: OppgaveBehandlingMetadata?,
)
