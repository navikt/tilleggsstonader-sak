package no.nav.tilleggsstonader.sak.behandling

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.utledBehandlingType
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.utledBehandlingTypeV2
import no.nav.tilleggsstonader.sak.behandling.OpprettBehandlingUtil.validerKanOppretteNyBehandling
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingKategori
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingId
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingIdRepository
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerMetadata
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.sak.behandling.vent.SettBehandlingPåVentRequest
import no.nav.tilleggsstonader.sak.behandling.vent.SettPåVentService
import no.nav.tilleggsstonader.sak.behandling.vent.ÅrsakSettPåVent
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class OpprettBehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val eksternBehandlingIdRepository: EksternBehandlingIdRepository,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val unleashService: UnleashService,
    private val settPåVentService: SettPåVentService,
    private val taskService: TaskService,
) {
    @Transactional
    fun opprettBehandling(request: OpprettBehandling): Behandling {
        brukerfeilHvis(request.kravMottatt != null && request.kravMottatt.isAfter(LocalDate.now())) {
            "Kan ikke sette krav mottattdato frem i tid"
        }
        feilHvisIkke(unleashService.isEnabled(Toggle.KAN_OPPRETTE_BEHANDLING)) {
            "Feature toggle for å opprette behandling er slått av"
        }

        val kanHaFlereBehandlingerPåSammeFagsak =
            unleashService.isEnabled(Toggle.KAN_HA_FLERE_BEHANDLINGER_PÅ_SAMME_FAGSAK)

        val tidligereBehandlinger = behandlingRepository.findByFagsakId(request.fagsakId)
        val forrigeBehandling = behandlingRepository.finnSisteIverksatteBehandling(request.fagsakId)
        val behandlingType =
            when (kanHaFlereBehandlingerPåSammeFagsak) {
                true -> utledBehandlingTypeV2(tidligereBehandlinger)
                false -> utledBehandlingType(tidligereBehandlinger)
            }

        validerKanOppretteNyBehandling(
            behandlingType = behandlingType,
            tidligereBehandlinger = tidligereBehandlinger,
            kanHaFlereBehandlingPåSammeFagsak = kanHaFlereBehandlingerPåSammeFagsak && request.tillatFlereÅpneBehandlinger,
        )

        val behandlingStatus = utledBehandlingStatus(tidligereBehandlinger)

        val behandling =
            behandlingRepository.insert(
                Behandling(
                    fagsakId = request.fagsakId,
                    forrigeIverksatteBehandlingId = forrigeBehandling?.id,
                    type = behandlingType,
                    steg = request.stegType,
                    status = behandlingStatus,
                    resultat = BehandlingResultat.IKKE_SATT,
                    årsak = request.behandlingsårsak,
                    kravMottatt = request.kravMottatt,
                    kategori = BehandlingKategori.NASJONAL,
                    nyeOpplysningerMetadata = request.nyeOpplysningerMetadata,
                ),
            )
        eksternBehandlingIdRepository.insert(EksternBehandlingId(behandlingId = behandling.id))

        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingshistorikk =
                Behandlingshistorikk(
                    behandlingId = behandling.id,
                    steg = request.stegType,
                    gitVersjon = Applikasjonsversjon.versjon,
                ),
        )

        if (behandlingStatus == BehandlingStatus.SATT_PÅ_VENT) {
            settPåVentService.settPåVent(
                behandling.id,
                SettBehandlingPåVentRequest(
                    årsaker = listOf(ÅrsakSettPåVent.ANNET),
                    frist = LocalDate.now().plusWeeks(1),
                    kommentar = "Behandling satt på vent grunnet det finnes en aktiv behandling på saken",
                    oppdaterOppgave = false,
                    beholdOppgave = false,
                ),
            )
        }

        if (request.oppgaveMetadata is OpprettBehandlingOppgaveMetadata.OppgaveMetadata) {
            taskService.save(
                OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                    OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                        behandlingId = behandling.id,
                        saksbehandler = request.oppgaveMetadata.tilordneSaksbehandler,
                        beskrivelse = request.oppgaveMetadata.beskrivelse,
                        // TODO - brukes for å opprette BehandlingsstatistikkTask.opprettMottattTask - bør heller opprette tasken her
                        hendelseTidspunkt = behandling.kravMottatt?.atStartOfDay() ?: LocalDateTime.now(),
                        prioritet = request.oppgaveMetadata.prioritet,
                    ),
                ),
            )
        }

        return behandling
    }

    private fun utledBehandlingStatus(tidligereBehandlinger: List<Behandling>): BehandlingStatus =
        if (tidligereBehandlinger.any { it.erAktiv() }) {
            BehandlingStatus.SATT_PÅ_VENT
        } else {
            BehandlingStatus.OPPRETTET
        }
}

data class OpprettBehandling(
    val fagsakId: FagsakId,
    val status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    val stegType: StegType = StegType.INNGANGSVILKÅR,
    val behandlingsårsak: BehandlingÅrsak,
    val kravMottatt: LocalDate? = null,
    val nyeOpplysningerMetadata: NyeOpplysningerMetadata? = null,
    val tillatFlereÅpneBehandlinger: Boolean = false,
    val oppgaveMetadata: OpprettBehandlingOppgaveMetadata,
)

sealed interface OpprettBehandlingOppgaveMetadata {
    data class OppgaveMetadata(
        val tilordneSaksbehandler: String?,
        val beskrivelse: String?,
        val prioritet: OppgavePrioritet,
    ) : OpprettBehandlingOppgaveMetadata

    data object UtenOppgave : OpprettBehandlingOppgaveMetadata
}
