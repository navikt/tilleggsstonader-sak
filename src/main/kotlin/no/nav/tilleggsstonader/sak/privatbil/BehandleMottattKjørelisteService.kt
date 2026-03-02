package no.nav.tilleggsstonader.sak.privatbil

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.sak.behandling.GjenbrukDataRevurderingService
import no.nav.tilleggsstonader.sak.behandling.OpprettBehandling
import no.nav.tilleggsstonader.sak.behandling.OpprettBehandlingOppgaveMetadata
import no.nav.tilleggsstonader.sak.behandling.OpprettBehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørelisteService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.DagligReiseVedtakService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandleMottattKjørelisteService(
    private val opprettBehandlingService: OpprettBehandlingService,
    private val gjenbrukDataRevurderingService: GjenbrukDataRevurderingService,
    private val dagligReiseVedtakService: DagligReiseVedtakService,
    private val avklartKjørelisteService: AvklartKjørelisteService,
    private val taskService: TaskService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun behandleMottattKjøreliste(kjøreliste: Kjøreliste) {
        // TODO: Toggle for å skru av?

        // Lag behandling av type "KJØRELISTE"
        // Kopier data fra forrige behandling -> oppdater ramme med nye satser?
        // Avklar uker så langt det går
        // Opprett behandle sak oppgave (automatisk senere)

        val behandling = opprettKjørelisteBehandling(kjøreliste)

        gjenbrukData(behandling)

        avklartKjørelisteService.avklarUkerFraKjøreliste(
            behandling = behandling,
            kjøreliste = kjøreliste,
        )

        taskService.save(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandlingId = behandling.id,
                    beskrivelse = "Skal behandles i TS-Sak",
                    prioritet = OppgavePrioritet.NORM,
                )
            )
        )
    }

    private fun opprettKjørelisteBehandling(kjøreliste: Kjøreliste): Behandling {
        logger.info("Oppretter kjørelistebehandling for fagsak=${kjøreliste.fagsakId}")

        return opprettBehandlingService.opprettBehandling(

            OpprettBehandling(
                fagsakId = kjøreliste.fagsakId,
                status = BehandlingStatus.OPPRETTET,
                stegType = StegType.KJØRELISTE, //TODO: Vurder denne
                behandlingsårsak = BehandlingÅrsak.KJØRELISTE,
                kravMottatt = kjøreliste.datoMottatt.toLocalDate(),
                oppgaveMetadata = OpprettBehandlingOppgaveMetadata.UtenOppgave
            )
        )
    }

    private fun gjenbrukData(behandling: Behandling) {
        val behandlingIdForGjenbruk = gjenbrukDataRevurderingService.finnBehandlingIdForGjenbruk(behandling.fagsakId)
        behandlingIdForGjenbruk?.let {
            gjenbrukDataRevurderingService.gjenbrukData(behandling, it)
            dagligReiseVedtakService.gjenbrukVedtak(forrigeIverksatteBehandlingId = it, nyBehandlingId = behandling.id)
        }
    }
}
