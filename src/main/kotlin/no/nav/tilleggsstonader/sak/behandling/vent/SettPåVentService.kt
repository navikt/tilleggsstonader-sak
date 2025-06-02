package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.OppdaterPåVentRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.SettPåVentRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.SettPåVentResponse
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.TaAvVentRequest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.statistikk.task.BehandlingsstatistikkTask
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.temporal.ChronoUnit

@Service
class SettPåVentService(
    private val behandlingService: BehandlingService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val oppgaveService: OppgaveService,
    private val taskService: TaskService,
    private val settPåVentRepository: SettPåVentRepository,
) {
    fun hentStatusSettPåVent(behandlingId: BehandlingId): StatusPåVentDto {
        val settPåVent = finnAktivSattPåVent(behandlingId)
        val oppgave = oppgaveService.hentOppgave(settPåVent.oppgaveId)

        val endret = utledEndretInformasjon(settPåVent)

        return StatusPåVentDto(
            årsaker = settPåVent.årsaker,
            kommentar = settPåVent.kommentar,
            datoSattPåVent = settPåVent.sporbar.opprettetTid.toLocalDate(),
            opprettetAv = settPåVent.sporbar.opprettetAv,
            endretAv = endret?.endretAv,
            endretTid = endret?.endretTid,
            frist = oppgave.fristFerdigstillelse,
            oppgaveVersjon = oppgave.versjonEllerFeil(),
        )
    }

    @Transactional
    fun settPåVent(
        behandlingId: BehandlingId,
        dto: SettPåVentDto,
    ): StatusPåVentDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        behandling.status.validerKanBehandlingRedigeres()

        behandlingService.markerBehandlingSomPåbegynt(behandlingId, behandling.status)

        opprettHistorikkInnslag(behandling, StegUtfall.SATT_PÅ_VENT, årsaker = dto.årsaker, kommentar = dto.kommentar)
        behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.SATT_PÅ_VENT, behandling.steg)

        val oppgave = hentOppgave(behandlingId)

        val settPåVent =
            SettPåVent(
                behandlingId = behandlingId,
                oppgaveId = oppgave.gsakOppgaveId,
                årsaker = dto.årsaker,
                kommentar = dto.kommentar,
            )
        settPåVentRepository.insert(settPåVent)

        val oppdatertOppgave = settPåVent(oppgave, dto)

        taskService.save(BehandlingsstatistikkTask.opprettVenterTask(behandlingId))
        val endret = utledEndretInformasjon(settPåVent)

        return StatusPåVentDto(
            årsaker = dto.årsaker,
            kommentar = dto.kommentar,
            datoSattPåVent = settPåVent.sporbar.opprettetTid.toLocalDate(),
            opprettetAv = settPåVent.sporbar.opprettetAv,
            endretAv = endret?.endretAv,
            endretTid = endret?.endretTid,
            frist = dto.frist,
            oppgaveVersjon = oppdatertOppgave.oppgaveVersjon,
        )
    }

    private fun settPåVent(
        oppgave: OppgaveDomain,
        dto: SettPåVentDto,
    ): SettPåVentResponse =
        oppgaveService.settPåVent(
            SettPåVentRequest(
                oppgaveId = oppgave.gsakOppgaveId,
                kommentar = dto.kommentar,
                frist = dto.frist,
                beholdOppgave = dto.beholdOppgave,
            ),
        )

    @Transactional
    fun oppdaterSettPåVent(
        behandlingId: BehandlingId,
        dto: OppdaterSettPåVentDto,
    ): StatusPåVentDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        feilHvis(behandling.status != BehandlingStatus.SATT_PÅ_VENT) {
            "Status på behandlingen må være ${BehandlingStatus.SATT_PÅ_VENT} for å kunne oppdatere"
        }
        val settPåVent = finnAktivSattPåVent(behandlingId)

        if (harEndretÅrsaker(settPåVent, dto) || harEndretKommentar(settPåVent, dto)) {
            opprettHistorikkInnslag(
                behandling,
                StegUtfall.SATT_PÅ_VENT,
                årsaker = dto.årsaker,
                kommentar = dto.kommentar,
            )
        }

        val oppdatertSettPåVent =
            settPåVentRepository.update(settPåVent.copy(årsaker = dto.årsaker, kommentar = dto.kommentar))

        val oppgaveResponse = oppdaterOppgave(oppdatertSettPåVent, dto)

        val endret = utledEndretInformasjon(oppdatertSettPåVent)

        return StatusPåVentDto(
            årsaker = oppdatertSettPåVent.årsaker,
            kommentar = oppdatertSettPåVent.kommentar,
            datoSattPåVent = oppdatertSettPåVent.sporbar.opprettetTid.toLocalDate(),
            opprettetAv = oppdatertSettPåVent.sporbar.opprettetAv,
            endretAv = endret?.endretAv,
            endretTid = endret?.endretTid,
            frist = dto.frist,
            oppgaveVersjon = oppgaveResponse.oppgaveVersjon,
        )
    }

    private fun oppdaterOppgave(
        settPåVent: SettPåVent,
        dto: OppdaterSettPåVentDto,
    ): SettPåVentResponse {
        val oppdatertOppgave =
            OppdaterPåVentRequest(
                oppgaveId = settPåVent.oppgaveId,
                oppgaveVersjon = dto.oppgaveVersjon,
                kommentar = dto.kommentar,
                frist = dto.frist,
                beholdOppgave = dto.beholdOppgave,
            )
        return oppgaveService.oppdaterPåVent(oppdatertOppgave)
    }

    /**
     * Det er ønskelig å vise om det ble endring på en SettPåVent. For å finne ut av det sjekkes det om tidspunktene er ulike.
     * Pga at opprettetTid og endretTid ikke helt er den samme er vi nøtt for å sjekke om den har blitt endret innen noen sekunder
     */
    private fun utledEndretInformasjon(oppdatertSettPåVent: SettPåVent) =
        oppdatertSettPåVent.sporbar
            .takeIf { ChronoUnit.SECONDS.between(it.opprettetTid, it.endret.endretTid) > 5 }
            ?.endret

    private fun harEndretKommentar(
        settPåVent: SettPåVent,
        dto: OppdaterSettPåVentDto,
    ) = settPåVent.kommentar != dto.kommentar

    private fun harEndretÅrsaker(
        settPåVent: SettPåVent,
        dto: OppdaterSettPåVentDto,
    ) = !settPåVent.årsaker.containsAll(dto.årsaker) ||
        settPåVent.årsaker.size != dto.årsaker.size

    private fun hentOppgave(behandlingId: BehandlingId): OppgaveDomain =
        oppgaveService.hentBehandleSakOppgaveSomIkkeErFerdigstilt(behandlingId)
            ?: error("Finner ikke behandleSakOppgave for behandling=$behandlingId")

    @Transactional
    fun taAvVent(
        behandlingId: BehandlingId,
        taAvVentDto: TaAvVentDto?,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        feilHvis(behandling.status != BehandlingStatus.SATT_PÅ_VENT) {
            "Kan ikke ta behandling av vent når status=${behandling.status}"
        }
        behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.UTREDES)

        val settPåVent = finnAktivSattPåVent(behandlingId).copy(aktiv = false, taAvVentKommentar = taAvVentDto?.kommentar)
        settPåVentRepository.update(settPåVent)

        opprettHistorikkInnslagTaAvVent(behandling, taAvVentDto?.kommentar)
        taOppgaveAvVent(settPåVent.oppgaveId, settPåVent, skalTilordnesRessurs = taAvVentDto?.skalTilordnesRessurs ?: true)
    }

    private fun finnAktivSattPåVent(behandlingId: BehandlingId) =
        settPåVentRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            ?: error("Finner ikke settPåVent for behandling=$behandlingId")

    private fun taOppgaveAvVent(
        oppgaveId: Long,
        settPåVent: SettPåVent,
        skalTilordnesRessurs: Boolean,
    ) {
        val taAvVent =
            TaAvVentRequest(
                oppgaveId = oppgaveId,
                beholdOppgave = skalTilordnesRessurs,
                kommentar = settPåVent.taAvVentKommentar,
            )
        oppgaveService.taAvVent(taAvVent)
    }

    private fun opprettHistorikkInnslag(
        behandling: Behandling,
        utfall: StegUtfall,
        kommentar: String?,
        årsaker: List<ÅrsakSettPåVent>,
    ) {
        val metadata: MutableMap<String, Any> =
            mutableMapOf(
                "årsaker" to årsaker,
            )
        kommentar?.let { metadata["kommentarSettPåVent"] = it }

        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingId = behandling.id,
            stegtype = behandling.steg,
            utfall = utfall,
            metadata = metadata,
        )
    }

    private fun opprettHistorikkInnslagTaAvVent(
        behandling: Behandling,
        kommentar: String?,
    ) {
        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingId = behandling.id,
            stegtype = behandling.steg,
            utfall = StegUtfall.TATT_AV_VENT,
            metadata = kommentar?.takeIf { it.isNotEmpty() }?.let { mapOf("kommentar" to it) },
        )
    }
}
