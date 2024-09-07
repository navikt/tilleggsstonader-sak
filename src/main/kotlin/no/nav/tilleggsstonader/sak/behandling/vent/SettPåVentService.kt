package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.oppgave.OppdatertOppgaveResponse
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveMappe
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.statistikk.task.BehandlingsstatistikkTask
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.temporal.ChronoUnit
import java.util.Optional

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
    fun settPåVent(behandlingId: BehandlingId, dto: SettPåVentDto): StatusPåVentDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke sette behandling på vent når status=${behandling.status}"
        }
        opprettHistorikkInnslag(behandling, StegUtfall.SATT_PÅ_VENT, årsaker = dto.årsaker, kommentar = dto.kommentar)
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
            BehandlingsstatistikkTask.opprettVenterTask(behandlingId),
        )
        val endret = utledEndretInformasjon(settPåVent)

        return StatusPåVentDto(
            årsaker = dto.årsaker,
            kommentar = dto.kommentar,
            datoSattPåVent = settPåVent.sporbar.opprettetTid.toLocalDate(),
            opprettetAv = settPåVent.sporbar.opprettetAv,
            endretAv = endret?.endretAv,
            endretTid = endret?.endretTid,
            frist = dto.frist,
            oppgaveVersjon = oppdatertOppgave.versjon,
        )
    }

    @Transactional
    fun oppdaterSettPåVent(behandlingId: BehandlingId, dto: OppdaterSettPåVentDto): StatusPåVentDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        feilHvis(behandling.status != BehandlingStatus.SATT_PÅ_VENT) {
            "Status på behandlingen må være ${BehandlingStatus.SATT_PÅ_VENT} for å kunne oppdatere"
        }
        val settPåVent = finnAktivSattPåVent(behandlingId)

        if (harEndretÅrsaker(settPåVent, dto)) {
            opprettHistorikkInnslag(
                behandling,
                StegUtfall.SATT_PÅ_VENT,
                årsaker = dto.årsaker,
                kommentar = dto.kommentar,
            )
        }
        val oppdatertSettPåVent =
            settPåVentRepository.update(settPåVent.copy(årsaker = dto.årsaker, kommentar = dto.kommentar))

        val oppgaveResponse = oppdaterOppgave(settPåVent, dto)

        val endret = utledEndretInformasjon(oppdatertSettPåVent)

        return StatusPåVentDto(
            årsaker = oppdatertSettPåVent.årsaker,
            kommentar = oppdatertSettPåVent.kommentar,
            datoSattPåVent = oppdatertSettPåVent.sporbar.opprettetTid.toLocalDate(),
            opprettetAv = oppdatertSettPåVent.sporbar.opprettetAv,
            endretAv = endret?.endretAv,
            endretTid = endret?.endretTid,
            frist = dto.frist,
            oppgaveVersjon = oppgaveResponse.versjon,
        )
    }

    /**
     * Det er ønskelig å vise om det ble endring på en SettPåVent. For å finne ut av det sjekkes det om tidspunktene er ulike.
     * Pga at opprettetTid og endretTid ikke helt er den samme er vi nøtt for å sjekke om den har blitt endret innen noen sekunder
     */
    private fun utledEndretInformasjon(oppdatertSettPåVent: SettPåVent) =
        oppdatertSettPåVent.sporbar.takeIf {
            ChronoUnit.SECONDS.between(it.opprettetTid, it.endret.endretTid) > 5
        }?.endret

    private fun harEndretÅrsaker(
        settPåVent: SettPåVent,
        dto: OppdaterSettPåVentDto,
    ) = !settPåVent.årsaker.containsAll(dto.årsaker) ||
        settPåVent.årsaker.size != dto.årsaker.size

    private fun hentOppgave(behandlingId: BehandlingId): Oppgave {
        val oppgave = hentBehandleSakOppgave(behandlingId)
        return oppgaveService.hentOppgave(oppgave.gsakOppgaveId)
    }

    @Transactional
    fun taAvVent(behandlingId: BehandlingId, taAvVentDto: TaAvVentDto?) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        feilHvis(behandling.status != BehandlingStatus.SATT_PÅ_VENT) {
            "Kan ikke ta behandling av vent når status=${behandling.status}"
        }
        behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.UTREDES)

        val settPåVent = finnAktivSattPåVent(behandlingId)
        settPåVentRepository.update(settPåVent.copy(aktiv = false, taAvVentKommentar = taAvVentDto?.kommentar))

        opprettHistorikkInnslagTaAvVent(behandling, taAvVentDto?.kommentar)
        taOppgaveAvVent(settPåVent.oppgaveId, skalTilordnesRessurs = taAvVentDto?.skalTilordnesRessurs ?: true)
    }

    private fun finnAktivSattPåVent(behandlingId: BehandlingId) =
        settPåVentRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            ?: error("Finner ikke settPåVent for behandling=$behandlingId")

    private fun settOppgavePåVent(
        behandlingId: BehandlingId,
        dto: SettPåVentDto,
    ): OppdatertOppgaveResponse {
        val oppgave = hentOppgave(behandlingId)

        val enhet = oppgave.tildeltEnhetsnr ?: error("Oppgave=${oppgave.id} mangler enhetsnummer")
        val mappe = oppgaveService.finnMappe(enhet, OppgaveMappe.PÅ_VENT)
        val oppdatertOppgave = Oppgave(
            id = oppgave.id,
            versjon = oppgave.versjon,
            tilordnetRessurs = "",
            fristFerdigstillelse = dto.frist,
            beskrivelse = SettPåVentBeskrivelseUtil.settPåVent(oppgave, dto.frist),
            mappeId = Optional.of(mappe.id),
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
            tilordnetRessurs = "",
        )
        return oppgaveService.oppdaterOppgave(oppdatertOppgave)
    }

    private fun taOppgaveAvVent(oppgaveId: Long, skalTilordnesRessurs: Boolean) {
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        val tilordnetRessurs = if (SikkerhetContext.erSaksbehandler() && skalTilordnesRessurs) {
            SikkerhetContext.hentSaksbehandler()
        } else {
            ""
        }

        val enhet = oppgave.tildeltEnhetsnr ?: error("Oppgave=${oppgave.id} mangler enhetsnummer")
        val mappeId = oppgaveService.finnMappe(enhet, OppgaveMappe.KLAR).id
        oppgaveService.oppdaterOppgave(
            Oppgave(
                id = oppgave.id,
                versjon = oppgave.versjon,
                tilordnetRessurs = tilordnetRessurs,
                fristFerdigstillelse = osloDateNow(),
                beskrivelse = SettPåVentBeskrivelseUtil.taAvVent(oppgave),
                mappeId = Optional.ofNullable(mappeId),
            ),
        )
    }

    private fun opprettHistorikkInnslag(
        behandling: Behandling,
        utfall: StegUtfall,
        kommentar: String?,
        årsaker: List<ÅrsakSettPåVent>,
    ) {
        val metadata: MutableMap<String, Any> = mutableMapOf(
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

    private fun hentBehandleSakOppgave(behandlingId: BehandlingId) =
        oppgaveService.hentBehandleSakOppgaveSomIkkeErFerdigstilt(behandlingId)
            ?: error("Finner ikke behandleSakOppgave for behandling=$behandlingId")
}
