package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.tilleggsstonader.kontrakter.oppgave.vent.TaAvVentRequest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.NullstillBehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaAvVentService(
    private val behandlingService: BehandlingService,
    private val nullstillBehandlingService: NullstillBehandlingService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val settPåVentRepository: SettPåVentRepository,
    private val oppgaveService: OppgaveService,
) {
    @Transactional
    fun taAvVent(
        behandlingId: BehandlingId,
        taAvVentDto: TaAvVentDto? = null,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId)

        when (val kanTaAvVent = utledTaAvVentStatus(behandling)) {
            is KanTaAvVent.Ja -> {
                behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.UTREDES)
                opprettHistorikkInnslagTaAvVent(behandling, taAvVentDto?.kommentar)

                when (kanTaAvVent.påkrevdHandling) {
                    PåkrevdHandling.Ingen -> {}
                    PåkrevdHandling.BehandlingMåNullstilles -> {
                        val nyForrigeBehandlingId =
                            behandlingService.finnSisteIverksatteBehandlingElseThrow(behandling.fagsakId)
                        nullstillBehandlingService.nullstillBehandling(behandlingId)
                        behandlingService.oppdaterForrigeIverksatteBehandlingId(
                            behandlingId,
                            nyForrigeBehandlingId,
                        )
                    }
                }
            }

            is KanTaAvVent.Nei -> {
                when (kanTaAvVent.årsak) {
                    Årsak.AnnenAktivBehandlingPåFagsaken ->
                        brukerfeil("Det finnes en annen aktiv behandling på fagsaken som må ferdigstilles eller settes på vent")

                    Årsak.ErAlleredePåVent -> feil("Behandlingen er allerede på vent")
                }
            }
        }

        val påVentMetadata =
            finnAktivSattPåVent(behandlingId).copy(aktiv = false, taAvVentKommentar = taAvVentDto?.kommentar)
        settPåVentRepository.update(påVentMetadata)

        taOppgaveAvVent(
            settPåVent = påVentMetadata,
            skalTilordnesRessurs = taAvVentDto?.skalTilordnesRessurs ?: true,
        )
    }

    fun kanTaAvVent(behandlingId: BehandlingId): KanTaAvVentDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        return KanTaAvVentDto.fraDomene(kanTaAvVent = utledTaAvVentStatus(behandling))
    }

    private fun utledTaAvVentStatus(behandling: Behandling): KanTaAvVent {
        if (behandling.status != BehandlingStatus.SATT_PÅ_VENT) {
            return KanTaAvVent.Nei(årsak = Årsak.ErAlleredePåVent)
        }
        val andreBehandlingerPåFagsaken =
            behandlingService.hentBehandlinger(behandling.fagsakId).filter { it.id != behandling.id }
        if (andreBehandlingerPåFagsaken.any { it.erAktiv() }) {
            return KanTaAvVent.Nei(årsak = Årsak.AnnenAktivBehandlingPåFagsaken)
        }
        val sisteIverksatte = behandlingService.finnSisteIverksatteBehandling(behandling.fagsakId)
        return if (sisteIverksatte == null || sisteIverksatte.id == behandling.forrigeIverksatteBehandlingId) {
            KanTaAvVent.Ja(påkrevdHandling = PåkrevdHandling.Ingen)
        } else {
            KanTaAvVent.Ja(påkrevdHandling = PåkrevdHandling.BehandlingMåNullstilles)
        }
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

    private fun taOppgaveAvVent(
        settPåVent: SettPåVent,
        skalTilordnesRessurs: Boolean,
    ) {
        val taAvVent =
            TaAvVentRequest(
                oppgaveId = settPåVent.oppgaveId,
                beholdOppgave = skalTilordnesRessurs,
                kommentar = settPåVent.taAvVentKommentar,
            )
        oppgaveService.taAvVent(taAvVent)
    }

    private fun finnAktivSattPåVent(behandlingId: BehandlingId) =
        settPåVentRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            ?: error("Finner ikke settPåVent for behandling=$behandlingId")
}

sealed class KanTaAvVent {
    data class Ja(
        val påkrevdHandling: PåkrevdHandling,
    ) : KanTaAvVent()

    data class Nei(
        val årsak: Årsak,
    ) : KanTaAvVent()
}

sealed class PåkrevdHandling {
    data object Ingen : PåkrevdHandling()

    data object BehandlingMåNullstilles : PåkrevdHandling()
}

sealed class Årsak {
    data object ErAlleredePåVent : Årsak()

    data object AnnenAktivBehandlingPåFagsaken : Årsak()
}
