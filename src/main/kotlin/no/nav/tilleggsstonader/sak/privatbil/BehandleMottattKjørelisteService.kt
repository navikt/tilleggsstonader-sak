package no.nav.tilleggsstonader.sak.privatbil

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
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
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
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
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val gjenbrukDataRevurderingService: GjenbrukDataRevurderingService,
    private val avklartKjørelisteService: AvklartKjørelisteService,
    private val taskService: TaskService,
    private val unleashService: UnleashService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun behandleMottattKjøreliste(kjøreliste: Kjøreliste) {
        val gjenbrukBehandling = finnKjørelistebehandlingSomKanGjenbrukes(kjøreliste)

        if (gjenbrukBehandling != null) {
            // Gjenbrukt behandling har allerede en oppgave-task fra da den ble opprettet.
            logger.info("Legger til kjøreliste=${kjøreliste.id} i behandling=${gjenbrukBehandling.id}")
            avklartKjørelisteService.avklarUkerFraKjøreliste(
                behandling = gjenbrukBehandling,
                kjøreliste = kjøreliste,
            )
        } else {
            opprettBehandlingFraKjøreliste(kjøreliste)
        }
    }

    private fun opprettBehandlingFraKjøreliste(kjøreliste: Kjøreliste) {
        val nyBehandling = opprettKjørelisteBehandling(kjøreliste)

        val skalOppretteAutomatiskTask = kanAutomatiskBehandles(nyBehandling.id)

        if (skalOppretteAutomatiskTask) {
            logger.info(
                "Ingen avvik funnet for behandling=${nyBehandling.id} og toggle er aktiv. Oppretter task for automatisk kjørelistebehandling",
            )
            behandlingService.oppdaterBehandlingMetode(
                nyBehandling.id,
                behandlingMetode = BehandlingMetode.AUTOMATISK,
            )
            taskService.save(AutomatiskKjørelisteBehandlingTask.opprettTask(nyBehandling.id))
        } else {
            logger.info("Oppretter manuell behandling for behandling=${nyBehandling.id}")
            taskService.save(
                OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                    OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                        behandlingId = nyBehandling.id,
                        beskrivelse = "Skal behandles i TS-Sak",
                        prioritet = OppgavePrioritet.NORM,
                    ),
                ),
            )
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

        avklartKjørelisteService.avklarUkerFraKjøreliste(
            behandling = kjørelistebehandling,
            kjøreliste = kjøreliste,
        )
        return kjørelistebehandling
    }

    private fun gjenbrukData(behandling: Behandling) {
        val behandlingIdForGjenbruk = gjenbrukDataRevurderingService.finnBehandlingIdForGjenbruk(behandling.fagsakId)
        behandlingIdForGjenbruk?.let {
            gjenbrukDataRevurderingService.gjenbrukData(behandling, it)
        }
    }

    private fun kanAutomatiskBehandles(behandlingId: BehandlingId): Boolean {
        val avklarteUker = avklartKjørelisteService.hentAvklarteUkerForBehandling(behandlingId)
        val harAvvik = avklarteUker.finnesUkerMedAvvik()
        val skalOppretteAutomatiskTask =
            !harAvvik && unleashService.isEnabled(Toggle.KAN_AUTOMATISK_BEHANDLE_KJØRELISTE)

        if (skalOppretteAutomatiskTask) {
            return true
        } else {
            return false
        }
    }
}
