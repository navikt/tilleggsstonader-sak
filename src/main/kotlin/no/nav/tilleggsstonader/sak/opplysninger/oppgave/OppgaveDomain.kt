package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("oppgave")
data class OppgaveDomain(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: BehandlingId?,
    val gsakOppgaveId: Long,
    val type: Oppgavetype,
    val status: Oppgavestatus,
    val tildeltEnhetsnummer: String?,
    val enhetsmappeId: Long?,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    val tilordnetSaksbehandler: String?,
) {
    fun erFerdigstilt() = status == Oppgavestatus.FERDIGSTILT
}

/**
 *
 * @param sendtTilTotrinnskontrollAv brukes for at saksbehandler ikke skal plukke behandlinger man selv sendt til totrinnskontroll

 * !!NOTER: Denne blir cachet i [OppgaveService.finnOppgaveMetadata]
 * så hvis noe legges til som kan endre seg, eks en status, så må cache fjernes
 */
data class OppgaveBehandlingMetadata(
    val gsakOppgaveId: Long,
    val behandlingId: UUID,
    val sendtTilTotrinnskontrollAv: String? = null,
    val erOpphor: Boolean? = false,
)

enum class Oppgavestatus {
    ÅPEN,
    FEILREGISTRERT,
    FERDIGSTILT,
}
