package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.oppgave.OppdatertOppgaveResponse
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.statistikk.task.BehandlingsstatistikkTask
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@Service
class SettPåVentService(
    private val behandlingService: BehandlingService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val oppgaveService: OppgaveService,
    private val taskService: TaskService,
    private val settPåVentRepository: SettPåVentRepository,
) {

    fun hentStatusSettPåVent(behandlingId: UUID): StatusPåVentDto {
        val settPåVent = finnAktivSattPåVent(behandlingId)
        val oppgave = oppgaveService.hentOppgave(settPåVent.oppgaveId)

        return StatusPåVentDto(
            årsaker = settPåVent.årsaker,
            kommentar = settPåVent.kommentar,
            frist = oppgave.fristFerdigstillelse,
            oppgaveVersjon = oppgave.versjonEllerFeil(),
        )
    }

    @Transactional
    fun settPåVent(behandlingId: UUID, dto: SettPåVentDto): StatusPåVentDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke sette behandling på vent når status=${behandling.status}"
        }
        opprettHistorikkInnslag(behandling, StegUtfall.SATT_PÅ_VENT, mapOf("årsaker" to dto.årsaker))
        behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.SATT_PÅ_VENT)

        val oppdatertOppgave = settOppgavePåVent(behandlingId, dto)
        val settPåVent = SettPåVent(
            behandlingId = behandlingId,
            oppgaveId = oppdatertOppgave.oppgaveId,
            årsaker = dto.årsaker,
            kommentar = dto.kommentar,
        )
        settPåVentRepository.insert(settPåVent)

        taskService.save(
            BehandlingsstatistikkTask.opprettVenterTask(behandlingId)
        )

        return StatusPåVentDto(
            årsaker = dto.årsaker,
            kommentar = dto.kommentar,
            frist = dto.frist,
            oppgaveVersjon = oppdatertOppgave.versjon,
        )
    }

    @Transactional
    fun oppdaterSettPåVent(behandlingId: UUID, dto: OppdaterSettPåVentDto): StatusPåVentDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        feilHvis(behandling.status != BehandlingStatus.SATT_PÅ_VENT) {
            "Status på behandlingen må være ${BehandlingStatus.SATT_PÅ_VENT} for å kunne oppdatere"
        }
        val settPåVent = finnAktivSattPåVent(behandlingId)

        if (harEndretÅrsaker(settPåVent, dto)) {
            opprettHistorikkInnslag(behandling, StegUtfall.SATT_PÅ_VENT, mapOf("årsaker" to dto.årsaker))
        }
        settPåVentRepository.update(settPåVent.copy(årsaker = dto.årsaker, kommentar = dto.kommentar))

        val oppgaveResponse = oppdaterOppgave(settPåVent, dto)

        return StatusPåVentDto(
            årsaker = dto.årsaker,
            kommentar = dto.kommentar,
            frist = dto.frist,
            oppgaveVersjon = oppgaveResponse.versjon,
        )
    }

    private fun harEndretÅrsaker(
        settPåVent: SettPåVent,
        dto: OppdaterSettPåVentDto,
    ) = !settPåVent.årsaker.containsAll(dto.årsaker) ||
            settPåVent.årsaker.size != dto.årsaker.size

    private fun hentOppgave(behandlingId: UUID): Oppgave {
        val oppgave = hentBehandleSakOppgave(behandlingId)
        return oppgaveService.hentOppgave(oppgave.gsakOppgaveId)
    }

    @Transactional
    fun taAvVent(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        feilHvis(behandling.status != BehandlingStatus.SATT_PÅ_VENT) {
            "Kan ikke ta behandling av vent når status=${behandling.status}"
        }
        behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.UTREDES)

        val settPåVent = finnAktivSattPåVent(behandlingId)
        settPåVentRepository.update(settPåVent.copy(aktiv = false))

        opprettHistorikkInnslag(behandling, StegUtfall.TATT_AV_VENT, null)
        taOppgaveAvVent(settPåVent.oppgaveId)

        taskService.save(
            BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId)
        )
    }

    private fun finnAktivSattPåVent(behandlingId: UUID) =
        settPåVentRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            ?: error("Finner ikke settPåVent for behandling=$behandlingId")

    private fun settOppgavePåVent(
        behandlingId: UUID,
        dto: SettPåVentDto,
    ): OppdatertOppgaveResponse {
        val oppgave = hentOppgave(behandlingId)
        // TODO må bruke enhet til saksbehandler, men 4462 er enheten til NAY. Enhetene til tiltak er uklart
        val mappeId = oppgaveService.finnMapper("4462").single { it.navn == "10 På vent" }.id.toLong()
        val oppdatertOppgave = Oppgave(
            id = oppgave.id,
            versjon = oppgave.versjon,
            tilordnetRessurs = "",
            fristFerdigstillelse = dto.frist,
            beskrivelse = SettPåVentBeskrivelseUtil.settPåVent(oppgave, dto.frist),
            mappeId = Optional.of(mappeId),
        )
        return oppgaveService.oppdaterOppgave(oppdatertOppgave)
    }

    private fun oppdaterOppgave(
        settPåVent: SettPåVent,
        dto: OppdaterSettPåVentDto,
    ): OppdatertOppgaveResponse {
        val oppgave = oppgaveService.hentOppgave(settPåVent.oppgaveId)
        val oppdatertOppgave = Oppgave(
            id = settPåVent.oppgaveId,
            versjon = dto.oppgaveVersjon,
            fristFerdigstillelse = dto.frist,
            beskrivelse = SettPåVentBeskrivelseUtil.oppdaterSettPåVent(oppgave, dto.frist),
        )
        return oppgaveService.oppdaterOppgave(oppdatertOppgave)
    }

    private fun taOppgaveAvVent(oppgaveId: Long) {
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        val tilordnetRessurs = if (SikkerhetContext.erSaksbehandler()) {
            SikkerhetContext.hentSaksbehandler()
        } else {
            ""
        }
        oppgaveService.oppdaterOppgave(
            Oppgave(
                id = oppgave.id,
                versjon = oppgave.versjon,
                tilordnetRessurs = tilordnetRessurs,
                fristFerdigstillelse = LocalDate.now(),
                beskrivelse = SettPåVentBeskrivelseUtil.taAvVent(oppgave),
                mappeId = Optional.empty(),
            ),
        )
    }

    private fun opprettHistorikkInnslag(
        behandling: Behandling,
        utfall: StegUtfall,
        metadata: Map<String, List<ÅrsakSettPåVent>>?,
    ) {
        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingId = behandling.id,
            stegtype = behandling.steg,
            utfall = utfall,
            metadata = metadata,
        )
    }

    private fun hentBehandleSakOppgave(behandlingId: UUID) =
        oppgaveService.hentBehandleSakOppgaveSomIkkeErFerdigstilt(behandlingId)
            ?: error("Finner ikke behandleSakOppgave for behandling=$behandlingId")
}
