package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import org.springframework.data.annotation.Id
import java.util.UUID

class TSOppgave(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val gsakOppgaveId: Long,
    val type: Oppgavetype,
    var erFerdigstilt: Boolean = false
)