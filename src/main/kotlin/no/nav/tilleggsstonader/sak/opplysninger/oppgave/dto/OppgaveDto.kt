package no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto

import java.util.UUID

data class OppgaveDto(
    val behandlingId: UUID?,
    val gsakOppgaveId: Long,
)
