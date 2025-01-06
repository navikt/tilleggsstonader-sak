package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.util.IdUtils
import no.nav.familie.prosessering.util.MDCConstants
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
@TaskStepBeskrivelse(
    taskStepType = IverksettBehandlingMånedTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 60L,
    beskrivelse = "Iverksetter behandling for en måned.",
)
class IverksettBehandlingMånedTask(
    private val behandlingService: BehandlingService,
    private val iverksettService: IverksettService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val taskData = objectMapper.readValue<TaskData>(task.payload)
        val iverksettingId = task.metadata.getProperty("iverksettingId") ?: error("Mangler iverksettingId")

        validerErSisteBehandling(taskData.behandlingId)

        val utbetalingsdato = taskData.utbetalingsdato
        feilHvis(utbetalingsdato > LocalDate.now()) {
            "Kan ikke iverksette for måned=$utbetalingsdato som er frem i tiden"
        }
        iverksettService.iverksett(taskData.behandlingId, UUID.fromString(iverksettingId), utbetalingsdato)
    }

    private fun validerErSisteBehandling(behandlingId: BehandlingId) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val sisteBehandling = behandlingService.finnSisteIverksatteBehandling(behandling.fagsakId)

        val vedtakstidspunkt = sisteBehandling?.vedtakstidspunktEllerFeil()

        feilHvis(vedtakstidspunkt != null && vedtakstidspunkt > behandling.vedtakstidspunktEllerFeil()) {
            "En revurdering har erstattet denne behandlingen. Denne tasken kan avvikshåndteres."
        }

        feilHvisIkke(behandling.id == sisteBehandling?.id) {
            "Behandling ${behandling.id} er ikke siste behandling for fagsak ${behandling.fagsakId}. Uklar årsak."
        }
    }

    companion object {

        fun opprettTask(behandlingId: BehandlingId, utbetalingsdato: LocalDate): Task {
            val properties = Properties().apply {
                setProperty("behandlingId", behandlingId.toString())
                setProperty("iverksettingId", UUID.randomUUID().toString())
                setProperty("utbetalingsdato", utbetalingsdato.toString())
                setProperty(MDCConstants.MDC_CALL_ID, IdUtils.generateId())
            }

            val payload = objectMapper.writeValueAsString(TaskData(behandlingId, utbetalingsdato))

            return Task(TYPE, payload).copy(metadataWrapper = PropertiesWrapper(properties))
        }

        const val TYPE = "IverksettBehandlingMåned"
    }

    private data class TaskData(
        val behandlingId: BehandlingId,
        val utbetalingsdato: LocalDate,
    )
}
