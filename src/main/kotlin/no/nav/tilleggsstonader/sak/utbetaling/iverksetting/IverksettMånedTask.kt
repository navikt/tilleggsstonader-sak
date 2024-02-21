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
import java.time.YearMonth
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
        // finn alle behandlinger som skal iverksettes for denne måneden som har andeler

        val måned = YearMonth.parse(task.payload)

        val behandlinger =
            andelTilkjentYtelseRepository.finnBehandlingerForIverksetting(sisteDatoIMåned = måned.atEndOfMonth())

        behandlinger.forEach {
            taskService.save(IverksettBehandlingMånedTask.opprettTask(behandlingId = it, måned = måned))
        }
    }

    override fun onCompletion(task: Task) {
        taskService.save(opprettTask(YearMonth.parse(task.payload).plusMonths(1)))
    }

    companion object {

        fun opprettTask(måned: YearMonth): Task {
            val properties = Properties().apply {
                setProperty("måned", måned.toString())
                setProperty(MDCConstants.MDC_CALL_ID, IdUtils.generateId())
            }

            return Task(TYPE, måned.toString()).copy(metadataWrapper = PropertiesWrapper(properties))
                .medTriggerTid(måned.atEndOfMonth().atTime(6, 4)) // TODO når skal vi iverksette
        }

        const val TYPE = "IverksettMåned"
    }
}
