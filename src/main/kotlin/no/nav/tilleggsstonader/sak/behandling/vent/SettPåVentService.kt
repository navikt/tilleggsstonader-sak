package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class SettPåVentService(
    private val behandlingService: BehandlingService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val oppgaveService: OppgaveService,
    private val settPåVentRepository: SettPåVentRepository,
) {

    fun hentSettPåVent(behandlingId: UUID): StatusPåVentDto {
        val settPåVent = settPåVentRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            ?: error("Finner ikke settPåVent for behandling=$behandlingId")
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

        val oppgave = hentOppgave(behandlingId)
        val oppdatertOppgave = Oppgave(
            id = oppgave.id,
            versjon = oppgave.versjon,
            tilordnetRessurs = "",
            fristFerdigstillelse = dto.frist,
            beskrivelse = SettPåVentBeskrivelseUtil.settPåVent(oppgave, dto),
        )
        val oppgaveResponse = oppgaveService.oppdaterOppgave(oppdatertOppgave)
        val settPåVent = SettPåVent(
            behandlingId = behandlingId,
            oppgaveId = oppgave.id,
            årsaker = dto.årsaker,
            kommentar = dto.kommentar,
        )
        settPåVentRepository.insert(settPåVent)

        return StatusPåVentDto(
            årsaker = dto.årsaker,
            kommentar = dto.kommentar,
            frist = dto.frist,
            oppgaveVersjon = oppgaveResponse.versjon,
        )
    }

    @Transactional
    fun oppdaterSettPåVent(behandlingId: UUID, dto: OppdaterSettPåVentDto): StatusPåVentDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        feilHvis(behandling.status != BehandlingStatus.SATT_PÅ_VENT) {
            "Status på behandlingen må være ${BehandlingStatus.SATT_PÅ_VENT} for å kunne oppdatere"
        }
        val settPåVent = settPåVentRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            ?: error("Finner ikke aktiv sett på vent for behandling=$behandlingId")

        if (!settPåVent.årsaker.containsAll(dto.årsaker) || settPåVent.årsaker.size != dto.årsaker.size) {
            opprettHistorikkInnslag(behandling, StegUtfall.SATT_PÅ_VENT, mapOf("årsaker" to dto.årsaker))
        }
        settPåVentRepository.update(settPåVent.copy(årsaker = dto.årsaker, kommentar = dto.kommentar))

        val oppgave = oppgaveService.hentOppgave(settPåVent.oppgaveId)
        val oppdatertOppgave =
            Oppgave(
                id = settPåVent.oppgaveId,
                versjon = dto.oppgaveVersjon,
                fristFerdigstillelse = dto.frist,
                beskrivelse = SettPåVentBeskrivelseUtil.oppdaterSettPåVent(oppgave, dto),
            )
        val oppgaveResponse = oppgaveService.oppdaterOppgave(oppdatertOppgave)

        // TODO vurder om man skal sende til DVH

        return StatusPåVentDto(
            årsaker = dto.årsaker,
            kommentar = dto.kommentar,
            frist = dto.frist,
            oppgaveVersjon = oppgaveResponse.versjon,
        )
    }

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

        val settPåVent = settPåVentRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            ?: error("Finner ikke aktiv sett på vent for behandling=$behandlingId")
        settPåVentRepository.update(settPåVent.copy(aktiv = false))
        opprettHistorikkInnslag(behandling, StegUtfall.TATT_AV_VENT, null)

        val oppgave = oppgaveService.hentOppgave(settPåVent.oppgaveId)
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
            ),
        )

        // TODO statistikk
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
