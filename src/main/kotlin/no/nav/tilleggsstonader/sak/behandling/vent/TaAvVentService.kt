package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.tilleggsstonader.kontrakter.oppgave.vent.TaAvVentRequest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.NullstillBehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandling.vent.KanTaAvVent.Ja.PåkrevdHandling
import no.nav.tilleggsstonader.sak.behandling.vent.KanTaAvVent.Nei.Årsak
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
            is KanTaAvVent.Nei -> {
                when (kanTaAvVent.årsak) {
                    Årsak.ErIkkePåVent -> feil("Behandlingen er ikke på vent")
                    Årsak.AnnenAktivBehandlingPåFagsaken -> brukerfeil("Det finnes allerede en aktiv behandling på denne fagsaken")
                }
            }
            is KanTaAvVent.Ja -> {
                val behandlingTattAvVent = behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.UTREDES)
                opprettHistorikkInnslagTaAvVent(behandling, taAvVentDto?.kommentar)

                when (kanTaAvVent.påkrevdHandling) {
                    PåkrevdHandling.Ingen -> {}
                    PåkrevdHandling.MåHåndtereNyttVedtakPåFagsaken ->
                        håndterAtNyBehandlingHarBlittFerdigstiltPåFagsaken(behandlingTattAvVent)
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
            return KanTaAvVent.Nei(årsak = Årsak.ErIkkePåVent)
        }

        if (detFinnesAndreAktiveBehandlingerPåFagsaken(behandling)) {
            return KanTaAvVent.Nei(årsak = Årsak.AnnenAktivBehandlingPåFagsaken)
        }

        return if (detHarBlittFattetVedtakPåFagsakenIMellomtiden(behandling)) {
            KanTaAvVent.Ja(påkrevdHandling = PåkrevdHandling.MåHåndtereNyttVedtakPåFagsaken)
        } else {
            KanTaAvVent.Ja(påkrevdHandling = PåkrevdHandling.Ingen)
        }
    }

    /**
     * Dersom behandling A har ligger på vent, og en annen behandling på fagsaken har fått et vedtak i mellomtiden, så
     * må behandling A nullstilles før den kan settes av vent, ettersom både vilkår og annen historikk kan ha endret
     * seg.
     */
    private fun håndterAtNyBehandlingHarBlittFerdigstiltPåFagsaken(behandlingSomTasAvVent: Behandling) {
        nullstillBehandlingService.nullstillBehandling(behandlingSomTasAvVent)
        val sisteIverksatteBehandlingId = behandlingService.finnSisteIverksatteBehandling(behandlingSomTasAvVent.fagsakId)
        behandlingService.gjørOmTilRevurdering(behandlingSomTasAvVent, sisteIverksatteBehandlingId?.id)
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

    private fun detFinnesAndreAktiveBehandlingerPåFagsaken(behandling: Behandling): Boolean =
        behandlingService
            .hentBehandlinger(behandling.fagsakId)
            .filter { it.id != behandling.id }
            .any { it.erAktiv() }

    private fun detHarBlittFattetVedtakPåFagsakenIMellomtiden(behandling: Behandling): Boolean {
        val sisteVedtakstidspunktPåFagsaken =
            behandlingService.finnSisteBehandlingSomHarVedtakPåFagsaken(behandling.fagsakId)?.vedtakstidspunkt
                ?: return false
        val tidspunktBehandlingenSistBleSattPåVent = finnAktivSattPåVent(behandling.id).sporbar.opprettetTid
        return sisteVedtakstidspunktPåFagsaken.isAfter(tidspunktBehandlingenSistBleSattPåVent)
    }

    private fun finnAktivSattPåVent(behandlingId: BehandlingId) =
        settPåVentRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            ?: error("Finner ikke settPåVent for behandling=$behandlingId")
}

sealed class KanTaAvVent {
    data class Ja(
        val påkrevdHandling: PåkrevdHandling,
    ) : KanTaAvVent() {
        sealed class PåkrevdHandling {
            data object Ingen : PåkrevdHandling()

            data object MåHåndtereNyttVedtakPåFagsaken : PåkrevdHandling()
        }
    }

    data class Nei(
        val årsak: Årsak,
    ) : KanTaAvVent() {
        sealed class Årsak {
            data object ErIkkePåVent : Årsak()

            data object AnnenAktivBehandlingPåFagsaken : Årsak()
        }
    }
}
