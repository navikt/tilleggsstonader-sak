package no.nav.tilleggsstonader.sak.integrasjonstest.extensions

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering

/**
 * Rest-endepunkter som endrer behandling krever at innlogget bruker er tilordnet oppgaven.
 */
fun IntegrationTest.opprettOgTilordneOppgaveForBehandling(
    behandlingId: BehandlingId,
    tilordneTilSaksbehandler: String? = testBrukerkontekst.bruker,
) {
    OpprettOppgaveForOpprettetBehandlingTask
        .opprettTask(
            OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                behandlingId = behandlingId,
                saksbehandler = tilordneTilSaksbehandler,
            ),
        ).also { taskService.save(it) }

    kjørTasksKlareForProsessering()
}

fun IntegrationTest.tilordneÅpenBehandlingOppgaveForBehandling(
    behandlingId: BehandlingId,
    tilordneTilSaksbehandler: String? = testBrukerkontekst.bruker,
) {
    val oppgaveDomain =
        oppgaveRepository
            .findByBehandlingId(behandlingId)
            .single { it.erÅpen() && it.erBehandlingsoppgave() && !it.erTilbakekrevingsoppgave() }
            .also { oppgaveRepository.update(it.copy(tilordnetSaksbehandler = tilordneTilSaksbehandler)) }

    val oppgave = mockClientService.oppgaveClient.finnOppgaveMedId(oppgaveDomain.gsakOppgaveId)
    mockClientService.oppgaveClient.fordelOppgave(oppgave.id, tilordneTilSaksbehandler, oppgave.versjon, null)
}
