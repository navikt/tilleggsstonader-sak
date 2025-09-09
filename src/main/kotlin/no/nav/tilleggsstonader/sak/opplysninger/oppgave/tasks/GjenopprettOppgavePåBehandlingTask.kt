package no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.vent.SettPåVentRepository
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveRepository
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.Oppgavestatus
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = GjenopprettOppgavePåBehandlingTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Brukes for å gjenopprette oppgave på behandling hvor oppgaven har blitt feilregistrert eller flyttet",
)
class GjenopprettOppgavePåBehandlingTask(
    private val behandligService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val oppgaveRepository: OppgaveRepository,
    private val settPåVentRepository: SettPåVentRepository,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        logger.info("Gjenoppretter oppgave for behandling ${task.payload}")
        val behandling = behandligService.hentBehandling(BehandlingId.fromString(task.payload))
        feilHvis(behandling.status.erFerdigbehandlet()) { "Behandling er ferdig behandlet" }

        val sisteOppgavePåBehandling = oppgaveService.finnSisteOppgaveForBehandling(behandling.id)

        val beskrivelseNyOppgave =
            when (sisteOppgavePåBehandling?.status == Oppgavestatus.FEILREGISTRERT) {
                true ->
                    "Opprinnelig oppgave er feilregistrert. For å kunne utføre behandling har det blitt opprettet en ny oppgave."
                false ->
                    "Opprinnelig oppgave har blitt flyttet eller endret. For å kunne utføre behandling har det blitt opprettet en ny oppgave."
            }

        if (sisteOppgavePåBehandling?.status == Oppgavestatus.ÅPEN) {
            settOppgaveTilIgnorert(sisteOppgavePåBehandling)
        }

        val opprettetOppgaveId = opprettNyOppgave(behandling, beskrivelseNyOppgave)

        // Om behandling er på vent så må vi oppdatere oppgaveId på settPåVent slik at saksbehandler får tatt behandlingen av vent
        settPåVentRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)?.let {
            logger.info(
                "Endrer oppgaveid på settPåVent for behandling ${behandling.id} til $opprettetOppgaveId fordi oppgaven gjenopprettes",
            )
            settPåVentRepository.update(it.copy(oppgaveId = opprettetOppgaveId))
        }
    }

    private fun settOppgaveTilIgnorert(oppgaveDomain: OppgaveDomain) {
        logger.info("Setter oppgave ${oppgaveDomain.id} på behandling ${oppgaveDomain.behandlingId} til ignorert")
        oppgaveRepository.update(oppgaveDomain.copy(status = Oppgavestatus.IGNORERT))
    }

    private fun opprettNyOppgave(
        behandling: Behandling,
        beskrivelse: String,
    ): Long =
        oppgaveService.opprettOppgave(
            behandling.id,
            OpprettOppgave(
                oppgavetype = finnOppgavetype(behandling),
                beskrivelse = beskrivelse,
                prioritet = OppgavePrioritet.HOY,
                fristFerdigstillelse = LocalDate.now(),
            ),
        )

    private fun finnOppgavetype(behandling: Behandling) =
        when (behandling.status) {
            BehandlingStatus.FATTER_VEDTAK -> Oppgavetype.GodkjenneVedtak
            else -> Oppgavetype.BehandleSak
        }

    companion object {
        const val TYPE = "GjenopprettOppgavePåBehandlingTask"

        fun opprettTask(behandlingId: BehandlingId): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties = Properties(),
            )
    }

    private fun BehandlingStatus.erFerdigbehandlet(): Boolean =
        when (this) {
            BehandlingStatus.FERDIGSTILT, BehandlingStatus.IVERKSETTER_VEDTAK -> true
            BehandlingStatus.FATTER_VEDTAK, BehandlingStatus.UTREDES, BehandlingStatus.SATT_PÅ_VENT, BehandlingStatus.OPPRETTET -> false
        }
}
