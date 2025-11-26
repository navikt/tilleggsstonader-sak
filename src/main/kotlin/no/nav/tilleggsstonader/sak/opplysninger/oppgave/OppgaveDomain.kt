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
    val tilbakekrevingBehandlingId: String?,
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

    fun erÅpen() = status == Oppgavestatus.ÅPEN

    fun erIgnorert() = status == Oppgavestatus.IGNORERT

    /**
     * En behandlingsoppgave er en oppgave som gjelder behandling av en behandling i ts-sak
     */
    fun erBehandlingsoppgave() = erBehandleSakOppgave() && !erTilbakekrevingsoppgave()

    private fun erBehandleSakOppgave() =
        type in
            listOf(
                Oppgavetype.BehandleSak,
                Oppgavetype.BehandleUnderkjentVedtak,
                Oppgavetype.GodkjenneVedtak,
            )

    fun erTilbakekrevingsoppgave() = tilbakekrevingBehandlingId != null
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

/**
 * Intern representasjon av oppgavestatus, reflekterer ikke nøyaktig status i oppgave ved ÅPEN eller IGNORERT
 */
enum class Oppgavestatus {
    ÅPEN,
    FEILREGISTRERT,
    FERDIGSTILT,

    /**
     * Brukes i tilfeller hvor vi skal se bort fra oppgaven, selv om den fortsatt er åpen-status i oppgave
     */
    IGNORERT,
}
