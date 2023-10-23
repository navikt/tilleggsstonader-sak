package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("oppgave")
data class OppgaveDomain(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val gsakOppgaveId: Long,
    val type: Oppgavetype,
    var erFerdigstilt: Boolean = false,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar()
)
