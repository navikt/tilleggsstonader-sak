package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.OppdaterPåVentRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.SettPåVentRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.SettPåVentResponse
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandling.vent.SettBehandlingPåVentOppgaveMetadata.OppdaterOppgave
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
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
        val settPåVent =
            finnAktivSattPåVent(behandlingId)
                ?: error("Finner ikke settPåVent for behandling=$behandlingId")
        val oppgave = oppgaveService.hentAktivBehandleSakOppgave(behandlingId)

        val endret = utledEndretInformasjon(settPåVent)

        return StatusPåVentDto(
            årsaker = settPåVent.årsaker,
            kommentar = settPåVent.kommentar,
            datoSattPåVent = settPåVent.sporbar.opprettetTid.toLocalDate(),
            opprettetAv = settPåVent.sporbar.opprettetAv,
            endretAv = endret?.endretAv,
            endretTid = endret?.endretTid,
            frist = oppgave.fristFerdigstillelse,
        )
    }

    @Transactional
    fun settPåVent(
        behandlingId: BehandlingId,
        request: SettBehandlingPåVent,
    ): StatusPåVentDto {
        val behandling = behandlingService.hentBehandling(behandlingId)

        if (behandling.status != BehandlingStatus.SATT_PÅ_VENT) {
            behandling.status.validerKanBehandlingRedigeres()
        }

        feilHvis(finnAktivSattPåVent(behandlingId) != null) {
            "Kan ikke gjøre endringer på denne behandlingen fordi den er satt på vent."
        }

        behandlingService.markerBehandlingSomPåbegyntHvisDenHarStatusOpprettet(behandlingId, behandling.status, behandling.steg)

        opprettHistorikkInnslag(behandling, årsaker = request.årsaker, kommentar = request.kommentar)
        behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.SATT_PÅ_VENT)

        val settPåVent =
            SettPåVent(
                behandlingId = behandlingId,
                årsaker = request.årsaker,
                kommentar = request.kommentar,
            )
        settPåVentRepository.insert(settPåVent)

        if (request.oppgaveMetadata is OppdaterOppgave) {
            settOppgavePåVent(behandlingId, request, request.oppgaveMetadata)
        }

        taskService.save(BehandlingsstatistikkTask.opprettVenterTask(behandlingId))
        val endret = utledEndretInformasjon(settPåVent)

        return StatusPåVentDto(
            årsaker = request.årsaker,
            kommentar = request.kommentar,
            datoSattPåVent = settPåVent.sporbar.opprettetTid.toLocalDate(),
            opprettetAv = settPåVent.sporbar.opprettetAv,
            endretAv = endret?.endretAv,
            endretTid = endret?.endretTid,
            frist = request.frist,
        )
    }

    private fun settOppgavePåVent(
        behandlingId: BehandlingId,
        request: SettBehandlingPåVent,
        oppdaterOppgave: OppdaterOppgave,
    ): SettPåVentResponse {
        val oppgave = oppgaveService.hentBehandleSakOppgaveDomainSomIkkeErFerdigstilt(behandlingId)
        return oppgaveService.settPåVent(
            SettPåVentRequest(
                oppgaveId = oppgave.gsakOppgaveId,
                kommentar = request.kommentar,
                frist = request.frist,
                beholdOppgave = oppdaterOppgave.beholdOppgave,
            ),
        )
    }

    @Transactional
    fun oppdaterSettPåVent(
        behandlingId: BehandlingId,
        dto: OppdaterSettPåVentDto,
    ): StatusPåVentDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        feilHvis(behandling.status != BehandlingStatus.SATT_PÅ_VENT) {
            "Status på behandlingen må være ${BehandlingStatus.SATT_PÅ_VENT} for å kunne oppdatere"
        }
        val settPåVent =
            finnAktivSattPåVent(behandlingId)
                ?: error("Finner ikke settPåVent for behandling=$behandlingId")

        if (harEndretÅrsaker(settPåVent, dto) || harEndretKommentar(settPåVent, dto)) {
            opprettHistorikkInnslag(
                behandling = behandling,
                årsaker = dto.årsaker,
                kommentar = dto.kommentar,
            )
        }

        val oppdatertSettPåVent =
            settPåVentRepository.update(settPåVent.copy(årsaker = dto.årsaker, kommentar = dto.kommentar))

        oppdaterOppgave(
            oppgaveId = oppgaveService.hentBehandleSakOppgaveDomainSomIkkeErFerdigstilt(behandlingId).gsakOppgaveId,
            dto = dto,
        )

        val endret = utledEndretInformasjon(oppdatertSettPåVent)

        return StatusPåVentDto(
            årsaker = oppdatertSettPåVent.årsaker,
            kommentar = oppdatertSettPåVent.kommentar,
            datoSattPåVent = oppdatertSettPåVent.sporbar.opprettetTid.toLocalDate(),
            opprettetAv = oppdatertSettPåVent.sporbar.opprettetAv,
            endretAv = endret?.endretAv,
            endretTid = endret?.endretTid,
            frist = dto.frist,
        )
    }

    private fun oppdaterOppgave(
        oppgaveId: Long,
        dto: OppdaterSettPåVentDto,
    ): SettPåVentResponse {
        val oppdatertOppgave =
            OppdaterPåVentRequest(
                oppgaveId = oppgaveId,
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

    private fun finnAktivSattPåVent(behandlingId: BehandlingId) = settPåVentRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)

    private fun opprettHistorikkInnslag(
        behandling: Behandling,
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
            utfall = StegUtfall.SATT_PÅ_VENT,
            metadata = metadata,
        )
    }
}
