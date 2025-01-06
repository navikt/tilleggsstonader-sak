package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.prosessering.util.IdUtils
import no.nav.familie.prosessering.util.MDCConstants
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelseRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = IverksettMånedTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 60L,
    beskrivelse = "Oppretter en task for hver iverksetting av en måned.",
)
class IverksettMånedTask(
    private val taskService: TaskService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        // finn alle behandlinger som skal iverksettes for gitt dato
        val utbetalingsdato = LocalDate.parse(task.payload)

        val behandlinger =
            andelTilkjentYtelseRepository.finnBehandlingerForIverksetting(utbetalingsdato = utbetalingsdato)

        behandlinger.forEach {
            taskService.save(IverksettBehandlingMånedTask.opprettTask(behandlingId = it, utbetalingsdato = utbetalingsdato))
        }
    }

    override fun onCompletion(task: Task) {
        taskService.save(opprettTask(LocalDate.now().plusDays(1)))
    }

    companion object {

        fun opprettTask(utbetalingsdato: LocalDate): Task {
            val properties = Properties().apply {
                setProperty("utbetalingsdato", utbetalingsdato.toString())
                setProperty(MDCConstants.MDC_CALL_ID, IdUtils.generateId())
            }

            return Task(TYPE, utbetalingsdato.toString()).copy(metadataWrapper = PropertiesWrapper(properties))
                .medTriggerTid(utbetalingsdato.atTime(6, 4))
        }

        const val TYPE = "IverksettMåned"
    }
}
