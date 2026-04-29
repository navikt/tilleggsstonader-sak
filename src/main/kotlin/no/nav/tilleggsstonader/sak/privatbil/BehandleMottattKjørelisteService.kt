package no.nav.tilleggsstonader.sak.privatbil

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.sak.behandling.GjenbrukDataRevurderingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingMetode
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.opprettelse.OpprettBehandling
import no.nav.tilleggsstonader.sak.behandling.opprettelse.OpprettBehandlingOppgaveMetadata
import no.nav.tilleggsstonader.sak.behandling.opprettelse.OpprettBehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørelisteService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandleMottattKjørelisteService(
    private val opprettBehandlingService: OpprettBehandlingService,
    private val behandlingRepository: BehandlingRepository,
    private val oppgaveService: OppgaveService,
    private val gjenbrukDataRevurderingService: GjenbrukDataRevurderingService,
    private val avklartKjørelisteService: AvklartKjørelisteService,
    private val taskService: TaskService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun behandleMottattKjøreliste(kjøreliste: Kjøreliste) {
        val behandling = finnKjørelistebehandlingSomKanGjenbrukes(kjøreliste) ?: opprettKjørelisteBehandling(kjøreliste)

        avklartKjørelisteService.avklarUkerFraKjøreliste(
            behandling = behandling,
            kjøreliste = kjøreliste,
        )
    }

    private fun finnKjørelistebehandlingSomKanGjenbrukes(kjøreliste: Kjøreliste): Behandling? {
        val sisteKjørelistebehandlingSomIkkeErPåbegynt =
            behandlingRepository
                .findByFagsakId(kjøreliste.fagsakId)
                .filter { it.type == BehandlingType.KJØRELISTE && it.status == BehandlingStatus.OPPRETTET }
                .sortedByDescending { it.sporbar.opprettetTid }
                .firstOrNull { !erPåbegynt(it) }

        sisteKjørelistebehandlingSomIkkeErPåbegynt?.let {
            logger.info("Gjenbruker eksisterende kjørelistebehandling=${it.id} for fagsak=${kjøreliste.fagsakId}")
        }

        return sisteKjørelistebehandlingSomIkkeErPåbegynt
    }

    private fun erPåbegynt(behandling: Behandling): Boolean {
        if (behandling.status != BehandlingStatus.OPPRETTET) {
            return true
        }

        val åpenBehandlingsoppgave = oppgaveService.hentÅpenBehandlingsoppgave(behandling.id) ?: return true

        return åpenBehandlingsoppgave.tilordnetSaksbehandler != null
    }

    private fun opprettKjørelisteBehandling(kjøreliste: Kjøreliste): Behandling {
        logger.info("Oppretter kjørelistebehandling for fagsak=${kjøreliste.fagsakId}")

        val kjørelistebehandling =
            opprettBehandlingService.opprettBehandling(
                OpprettBehandling(
                    fagsakId = kjøreliste.fagsakId,
                    status = BehandlingStatus.OPPRETTET,
                    stegType = StegType.KJØRELISTE,
                    behandlingsårsak = BehandlingÅrsak.KJØRELISTE,
                    behandlingMetode = BehandlingMetode.MANUELL,
                    kravMottatt = kjøreliste.datoMottatt.toLocalDate(),
                    oppgaveMetadata = OpprettBehandlingOppgaveMetadata.UtenOppgave,
                ),
            )

        gjenbrukData(kjørelistebehandling)
        taskService.save(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandlingId = kjørelistebehandling.id,
                    beskrivelse = "Skal behandles i TS-Sak",
                    prioritet = OppgavePrioritet.NORM,
                ),
            ),
        )

        return kjørelistebehandling
    }

    private fun gjenbrukData(behandling: Behandling) {
        val behandlingIdForGjenbruk = gjenbrukDataRevurderingService.finnBehandlingIdForGjenbruk(behandling.fagsakId)
        behandlingIdForGjenbruk?.let {
            gjenbrukDataRevurderingService.gjenbrukData(behandling, it)
        }
    }
}
