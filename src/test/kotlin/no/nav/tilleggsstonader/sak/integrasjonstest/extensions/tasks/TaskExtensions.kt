package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.FerdigstillOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import org.springframework.data.domain.Pageable

// Returnerer tasks som er blitt kjørt
fun IntegrationTest.kjørTasksKlareForProsessering() {
    logger.info("Kjører tasks klare for prosessering")
    taskService
        .finnAlleTasksKlareForProsessering(Pageable.unpaged())
//        .filterNot { it.type == BehandlingsstatistikkTask.TYPE } // Tester ikke statistikkutsendelse her todo fjerne?
        .forEach { kjørTask(it) }
    logger.info("Tasks kjørt OK")
}

fun IntegrationTest.kjørTask(task: Task) {
    try {
        taskWorker.markerPlukket(task.id)
        logger.info("Kjører task ${task.id} type=${task.type} msg=${taskMsg(task)}")
        taskWorker.doActualWork(task.id)
    } catch (e: Exception) {
        logger.error("Feil ved kjøring av task ${task.id} type=${task.type} msg=${taskMsg(task)}", e)
    }
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
