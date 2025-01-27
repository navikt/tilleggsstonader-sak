package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.prosessering.util.IdUtils
import no.nav.familie.prosessering.util.MDCConstants
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = DagligIverksettTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 60L,
    beskrivelse = """
        Jobb som kjøres hver dag som oppretter en ny task for hver behandling 
        som har andeler som skal iverksettes den dagen
        """,
)
class DagligIverksettTask(
    private val taskService: TaskService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        /**
         * Skal bruke dagens dato som utbetalingsdato.
         * Trenger ikke å bruke utbetalingsdato fra metadata eller payload, eks i tilfelle tasken feiler og kjører neste dag så skal nytt dagens dato brukes
         */
        val utbetalingsdato = LocalDate.now()

        val behandlinger =
            andelTilkjentYtelseRepository.finnBehandlingerForIverksetting(utbetalingsdato = utbetalingsdato)

        behandlinger.forEach {
            taskService.save(DagligIverksettBehandlingTask.opprettTask(behandlingId = it, utbetalingsdato = utbetalingsdato))
        }
    }

    override fun onCompletion(task: Task) {
        taskService.save(opprettTask(LocalDate.now().plusDays(1).datoEllerNesteMandagHvisLørdagEllerSøndag()))
    }

    companion object {
        fun opprettTask(utbetalingsdato: LocalDate): Task {
            val properties =
                Properties().apply {
                    setProperty("utbetalingsdato", utbetalingsdato.toString())
                    setProperty(MDCConstants.MDC_CALL_ID, IdUtils.generateId())
                }

            return Task(TYPE, utbetalingsdato.toString())
                .copy(metadataWrapper = PropertiesWrapper(properties))
                .medTriggerTid(utbetalingsdato.atTime(6, 4))
        }

        const val TYPE = "DagligIverksett"
    }
}
