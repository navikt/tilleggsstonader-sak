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
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.finnesUkerMedAvvik
import no.nav.tilleggsstonader.sak.privatbil.task.AutomatiskKjørelisteBehandlingTask
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
        val gjenbrukBehandling = finnKjørelistebehandlingSomKanGjenbrukes(kjøreliste)
        var behandling = gjenbrukBehandling ?: opprettKjørelisteBehandling(kjøreliste)

        avklartKjørelisteService.avklarUkerFraKjøreliste(
            behandling = behandling,
            kjøreliste = kjøreliste,
        )

        val avklarteUker = avklartKjørelisteService.hentAvklarteUkerForBehandling(behandling.id)
        val harAvvik = avklarteUker.finnesUkerMedAvvik()

        if (gjenbrukBehandling == null && !harAvvik && behandling.behandlingMetode != BehandlingMetode.AUTOMATISK) {
            behandling = behandling.copy(behandlingMetode = BehandlingMetode.AUTOMATISK)
            behandlingRepository.update(behandling)
        }

        if (harAvvik) {
            // Gjenbrukt behandling har allerede en oppgave-task fra da den ble opprettet
            if (gjenbrukBehandling == null) {
                taskService.save(
                    OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                        OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                            behandlingId = behandling.id,
                            beskrivelse = "Skal behandles i TS-Sak",
                            prioritet = OppgavePrioritet.NORM,
                        ),
                    ),
                )
            }
        } else {
            logger.info("Ingen avvik funnet for behandling=${behandling.id}. Oppretter task for automatisk kjørelistebehandling")
            taskService.save(AutomatiskKjørelisteBehandlingTask.opprettTask(behandling.id))
        }
    }

    private fun finnKjørelistebehandlingSomKanGjenbrukes(kjøreliste: Kjøreliste): Behandling? =
        behandlingRepository
            .findByFagsakId(kjøreliste.fagsakId)
            .filter { it.type == BehandlingType.KJØRELISTE && it.status == BehandlingStatus.OPPRETTET }
            .sortedByDescending { it.sporbar.opprettetTid }
            .firstOrNull { !erPåbegynt(it) }
            ?.also { logger.info("Gjenbruker eksisterende kjørelistebehandling=${it.id} for fagsak=${kjøreliste.fagsakId}") }

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

        return kjørelistebehandling
    }

    private fun gjenbrukData(behandling: Behandling) {
        val behandlingIdForGjenbruk = gjenbrukDataRevurderingService.finnBehandlingIdForGjenbruk(behandling.fagsakId)
        behandlingIdForGjenbruk?.let {
            gjenbrukDataRevurderingService.gjenbrukData(behandling, it)
        }
    }
}
