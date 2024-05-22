package no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto

import java.util.UUID

data class OppgaveMedBehandlingIdDto(
    val behandlingId: UUID?,
    val gsakOppgaveId: Long,
)
