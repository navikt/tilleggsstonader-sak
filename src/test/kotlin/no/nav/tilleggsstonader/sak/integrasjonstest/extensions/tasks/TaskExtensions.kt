package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.libs.log.logger
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.FerdigstillOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import org.assertj.core.api.Assertions.assertThat
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

fun IntegrationTest.kjørTasksKlareForProsesseringTilIngenTasksIgjen() {
    do {
        kjørTasksKlareForProsessering()
    } while (taskService.finnAlleTasksKlareForProsessering(Pageable.unpaged()).isNotEmpty())
}

fun IntegrationTest.finnAlleTaskerMedType(type: String) = taskService.finnAlleTaskerMedType(type)

fun IntegrationTest.kjørTasksKlareForProsessering() {
    logger.info("Kjører tasks klare for prosessering")
    taskService
        .finnAlleTasksKlareForProsessering(Pageable.unpaged())
        .sortedBy { it.opprettetTid }
        .forEach { kjørTask(it) }
    logger.info("Tasks kjørt OK")
}

fun IntegrationTest.kjørTask(task: Task) {
    try {
        taskWorker.markerPlukket(task.id)
        logger.info("Kjører task ${task.id} type=${task.type} msg=${taskMsg(task)}")
        taskWorker.doActualWork(task.id)
    } catch (e: RekjørSenereException) {
        logger.warn("RekjørSenereException for task ${task.id} type=${task.type} msg=${taskMsg(task)}", e)
        taskWorker.rekjørSenere(task.id, e)
    } catch (e: Exception) {
        logger.error("Feil ved kjøring av task ${task.id} type=${task.type} msg=${taskMsg(task)}", e)
        taskWorker.doFeilhåndtering(task.id, e)
    }
}

fun IntegrationTest.kjørAlleTaskMedSenererTriggertid() {
    taskService
        .findAll()
        .filter {
            it.status != Status.FERDIG &&
                it.status != Status.PLUKKET &&
                it.triggerTid > LocalDateTime.now()
        }.map { task -> taskService.save(task.copy(triggerTid = LocalDateTime.now())) }
        .forEach { kjørTask(it) }
}

private fun taskMsg(it: Task): String =
    when (it.type) {
        OpprettOppgaveTask.TYPE ->
            objectMapper
                .readValue<OpprettOppgaveTask.OpprettOppgaveTaskData>(it.payload)
                .let { "type=${it.oppgave.oppgavetype} kobling=${it.kobling}" }

        FerdigstillOppgaveTask.TYPE ->
            objectMapper
                .readValue<FerdigstillOppgaveTask.FerdigstillOppgaveTaskData>(it.payload)
                .let { "type=${it.oppgavetype} behandling=${it.behandlingId}" }

        else -> it.payload
    }

fun IntegrationTest.assertFinnesTaskMedType(
    type: String,
    antall: Int = 1,
) {
    assertThat(taskService.finnAlleTasksKlareForProsessering(Pageable.unpaged()).filter { it.type == type }).hasSize(
        antall,
    )
}
