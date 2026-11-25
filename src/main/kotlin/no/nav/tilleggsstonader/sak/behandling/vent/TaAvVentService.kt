package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.tilleggsstonader.kontrakter.felles.behandlendeEnhet
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.TaAvVentRequest
import no.nav.tilleggsstonader.libs.feil.brukerfeil
import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.NullstillBehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandling.vent.KanTaAvVent.Ja.PåkrevdHandling
import no.nav.tilleggsstonader.sak.behandling.vent.KanTaAvVent.Nei.Årsak
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.fristBehandleSakOppgave
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TaAvVentService(
    private val behandlingService: BehandlingService,
    private val nullstillBehandlingService: NullstillBehandlingService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val settPåVentRepository: SettPåVentRepository,
    private val oppgaveService: OppgaveService,
    private val fagsakService: FagsakService,
) {
    @Transactional
    fun taAvVent(
        behandlingId: BehandlingId,
        taAvVentDto: TaAvVentDto,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val aktivSettPåVent = finnAktivSettPåVent(behandlingId)

        when (val kanTaAvVent = utledTaAvVentStatus(behandling)) {
            is KanTaAvVent.Nei -> {
                when (kanTaAvVent.årsak) {
                    Årsak.ErIkkePåVent -> feil("Behandlingen er ikke på vent")
                    Årsak.AnnenAktivBehandlingPåFagsaken -> brukerfeil("Det finnes allerede en aktiv behandling på denne fagsaken")
                }
            }

            is KanTaAvVent.Ja -> {
                val oppdatertBehandling =
                    behandlingService
                        .oppdaterStatusPåBehandling(
                            behandlingId,
                            taAvVentDto.settBehandlingStatusTil,
                        )

                if (!aktivSettPåVent!!.erSattPåVentForSatsjustering()) {
                    opprettHistorikkInnslagTaAvVent(oppdatertBehandling, taAvVentDto.kommentar)
                }

                when (kanTaAvVent.påkrevdHandling) {
                    PåkrevdHandling.Ingen -> Unit
                    PåkrevdHandling.MåHåndtereNyttVedtakPåFagsaken ->
                        håndterAtNyBehandlingHarBlittFerdigstiltPåFagsaken(oppdatertBehandling)
                    PåkrevdHandling.MåOppdatereBehandlingstype ->
                        behandlingService.oppdaterReferanseTilForrigeIverksatteBehandlingOgOppdaterType(
                            oppdatertBehandling,
                            oppdatertBehandling.forrigeIverksatteBehandlingId,
                        )
                }
            }
        }

        val oppdatertSettPåVent =
            aktivSettPåVent.copy(aktiv = false, taAvVentKommentar = taAvVentDto.kommentar)
        settPåVentRepository.update(oppdatertSettPåVent)

        if (!oppdatertSettPåVent.erSattPåVentForSatsjustering()) {
            val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
            taOppgaveAvVent(
                settPåVent = oppdatertSettPåVent,
                skalTilordnesRessurs = taAvVentDto.skalTilordnesRessurs,
                frist =
                    fristBehandleSakOppgave(
                        kravMottatt = behandling.kravMottatt,
                        behandlingOpprettet = behandling.sporbar.opprettetTid,
                    ),
                endretAvEnhetsnr = fagsak.stønadstype.behandlendeEnhet().enhetsnr,
            )
        }
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

        val sisteIverksatteBehandlingId = behandlingService.finnSisteIverksatteBehandling(behandling.fagsakId)?.id
        return when {
            sisteIverksatteBehandlingId != behandling.forrigeIverksatteBehandlingId ->
                KanTaAvVent.Ja(påkrevdHandling = PåkrevdHandling.MåHåndtereNyttVedtakPåFagsaken)
            måOppdatereBehandlingstype(behandling) ->
                KanTaAvVent.Ja(påkrevdHandling = PåkrevdHandling.MåOppdatereBehandlingstype)
            else -> KanTaAvVent.Ja(påkrevdHandling = PåkrevdHandling.Ingen)
        }
    }

    /**
     * Endret iverksettingsreferanse krever nullstilling og gjenbruk av oppdatert grunnlag.
     * Et førstegangsavslag uten endret referanse krever bare typeendring, som håndteres separat.
     *
     * For tilsyn barn må også manglende barn fra forrige behandling kopieres når stønadsvilkårene gjenbrukes.
     */
    private fun håndterAtNyBehandlingHarBlittFerdigstiltPåFagsaken(behandlingSomTasAvVent: Behandling) {
        val sisteIverksatteBehandlingId = behandlingService.finnSisteIverksatteBehandling(behandlingSomTasAvVent.fagsakId)

        nullstillBehandlingService.nullstillBehandling(behandlingSomTasAvVent)
        behandlingService.oppdaterReferanseTilForrigeIverksatteBehandlingOgOppdaterType(
            behandlingSomTasAvVent,
            sisteIverksatteBehandlingId?.id,
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

    private fun taOppgaveAvVent(
        settPåVent: SettPåVent,
        skalTilordnesRessurs: Boolean,
        frist: LocalDate,
        endretAvEnhetsnr: String?,
    ) {
        val oppgaveId = oppgaveService.hentBehandleSakOppgaveDomainSomIkkeErFerdigstilt(settPåVent.behandlingId).gsakOppgaveId
        val taAvVent =
            TaAvVentRequest(
                oppgaveId = oppgaveId,
                beholdOppgave = skalTilordnesRessurs,
                kommentar = settPåVent.taAvVentKommentar,
                frist = frist,
                endretAvEnhetsnr = endretAvEnhetsnr,
            )
        oppgaveService.taAvVent(taAvVent)
    }

    private fun detFinnesAndreAktiveBehandlingerPåFagsaken(behandling: Behandling): Boolean =
        behandlingService
            .hentBehandlinger(behandling.fagsakId)
            .filter { it.id != behandling.id }
            .any { it.erAktiv() }

    private fun måOppdatereBehandlingstype(behandling: Behandling): Boolean {
        if (behandling.type != BehandlingType.FØRSTEGANGSBEHANDLING) {
            return false
        }

        return behandlingService.hentBehandlinger(behandling.fagsakId).any {
            it.id != behandling.id &&
                it.type == BehandlingType.FØRSTEGANGSBEHANDLING &&
                it.status == BehandlingStatus.FERDIGSTILT &&
                it.resultat == BehandlingResultat.AVSLÅTT
        }
    }

    private fun finnAktivSettPåVent(behandlingId: BehandlingId) = settPåVentRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
}

sealed class KanTaAvVent {
    data class Ja(
        val påkrevdHandling: PåkrevdHandling,
    ) : KanTaAvVent() {
        sealed class PåkrevdHandling {
            data object Ingen : PåkrevdHandling()

            data object MåHåndtereNyttVedtakPåFagsaken : PåkrevdHandling()

            data object MåOppdatereBehandlingstype : PåkrevdHandling()
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
