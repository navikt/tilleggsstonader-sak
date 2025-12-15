package no.nav.tilleggsstonader.sak.integrasjonstest.extensions

import no.nav.tilleggsstonader.kontrakter.felles.Enhet
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.OppgaveClientMockConfig.Companion.journalføringsoppgaveRequest
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.tilNyOppgave
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
            .single { it.erÅpen() && it.erBehandlingsoppgave() }
            .also { oppgaveRepository.update(it.copy(tilordnetSaksbehandler = tilordneTilSaksbehandler)) }

    val oppgave = mockClientService.oppgaveClient.finnOppgaveMedId(oppgaveDomain.gsakOppgaveId)
    mockClientService.oppgaveClient.fordelOppgave(oppgave.id, tilordneTilSaksbehandler, oppgave.versjon, null)
}

fun IntegrationTest.opprettJournalføringsoppgave(
    tema: Tema = Tema.TSO,
    behandlingstema: String = "ab0300",
    tildeltEnhetsnummer: String = Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD.enhetsnr,
    journalpostId: String,
): Oppgave {
    val oppgave =
        journalføringsoppgaveRequest(
            tema = tema,
            behandlingstema = behandlingstema,
            tildeltEnhetsnummer = tildeltEnhetsnummer,
            journalpostId = journalpostId,
        ).tilNyOppgave()
    return mockClientService.oppgavelager.leggTilOppgave(oppgave)
}
