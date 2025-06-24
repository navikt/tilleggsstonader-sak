package no.nav.tilleggsstonader.sak.opplysninger.saksbehandler

import no.nav.tilleggsstonader.kontrakter.felles.Saksbehandler
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.StatusEnum
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveClient
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveRepository
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.SaksbehandlerDto
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.SaksbehandlerRolle
import org.springframework.stereotype.Service

@Service
class SaksbehandlerService(
    private val oppgaveClient: OppgaveClient,
    private val oppgaveRepository: OppgaveRepository,
    private val behandlingRepository: BehandlingRepository,
    private val saksbehandlerClient: SaksbehandlerClient,
) {
    /**
     *  I tilfeller hvor saksbehandler manuelt oppretter en revurdering eller en førstegangsbehandling vil oppgaven
     *  som returneres fra oppgavesystemet være null. Dette skjer fordi oppgavesystemet bruker litt tid av variabel
     *  lengde på å opprette den tilhørende behandle-sak-oppgaven til den opprettede behandlingen.
     *
     * [no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.SaksbehandlerRolle.OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER]: Dersom null blir returnert og
     * behandlingen befinner seg i steget REVURDERING_ÅRSAK, VILKÅR eller BEREGNE_YTELSE anser vi det som svært
     * sannsynlig at det er den innloggede saksbehandleren som er ansvarlig for behandlingen - oppgaven har bare ikke
     * rukket å bli opprettet enda. I dette tilfellet returnerer vi OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER
     * til frontend.
     *
     * [no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.SaksbehandlerRolle.OPPGAVE_FINNES_IKKE]: Dersom null returneres og behandlingen ikke befinner seg i et av de nevnte
     * stegene returnerer vi OPPGAVE_FINNES_IKKE til frontend.
     */

    fun finnSaksbehandler(behandlingId: BehandlingId): SaksbehandlerDto? {
        val oppgave = hentIkkeFerdigstiltOppgaveForBehandlingGittStegtype(behandlingId)
        return utledAnsvarligSaksbehandlerForOppgave(behandlingId, oppgave)
    }

    fun tilordnetRessursErInnloggetSaksbehandler(
        behandlingId: BehandlingId,
        oppgavetyper: Set<Oppgavetype> = setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak),
    ): Boolean {
        val oppgave =
            if (erUtviklerMedVeilderrolle()) {
                null
            } else {
                hentIkkeFerdigstiltOppgaveForBehandling(
                    behandlingId,
                    oppgavetyper,
                )
            }

        val rolle = utledSaksbehandlerRolle(behandlingId, oppgave)

        return when (rolle) {
            SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER, SaksbehandlerRolle.OPPGAVE_FINNES_IKKE,
            @Suppress("ktlint:standard:max-line-length")
            SaksbehandlerRolle.OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER,
            -> true

            else -> false
        }
    }

    fun hentIkkeFerdigstiltOppgaveForBehandling(
        behandlingId: BehandlingId,
        oppgavetyper: Set<Oppgavetype> =
            setOf(
                Oppgavetype.BehandleSak,
                Oppgavetype.BehandleUnderkjentVedtak,
                Oppgavetype.GodkjenneVedtak,
            ),
    ): Oppgave? =
        hentOppgaveSomIkkeErFerdigstilt(behandlingId, oppgavetyper)
            ?.let { oppgaveClient.finnOppgaveMedId(it.gsakOppgaveId) }

    fun hentIkkeFerdigstiltOppgaveForBehandlingGittStegtype(behandlingId: BehandlingId): Oppgave? {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val oppgavetyper =
            when (behandling.steg.tillattFor) {
                BehandlerRolle.SAKSBEHANDLER -> setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak)
                BehandlerRolle.BESLUTTER -> setOf(Oppgavetype.GodkjenneVedtak)
                else -> emptySet()
            }
        return hentOppgaveSomIkkeErFerdigstilt(behandlingId, oppgavetyper)
            ?.let { oppgaveClient.finnOppgaveMedId(it.gsakOppgaveId) }
    }

    fun hentOppgaveSomIkkeErFerdigstilt(
        behandlingId: BehandlingId,
        oppgavetyper: Set<Oppgavetype>,
    ): OppgaveDomain? =
        oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
            behandlingId,
            oppgavetyper,
        )

    fun utledAnsvarligSaksbehandlerForOppgave(
        behandlingId: BehandlingId,
        behandleSakOppgave: Oppgave?,
    ): SaksbehandlerDto {
        val rolle = utledSaksbehandlerRolle(behandlingId, behandleSakOppgave)

        val tilordnetSaksbehandler =
            if (rolle == SaksbehandlerRolle.OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER) {
                hentSaksbehandlerInfo(SikkerhetContext.hentSaksbehandler())
            } else {
                behandleSakOppgave?.tilordnetRessurs?.let { hentSaksbehandlerInfo(it) }
            }

        return SaksbehandlerDto(
            etternavn = tilordnetSaksbehandler?.etternavn ?: "",
            fornavn = tilordnetSaksbehandler?.fornavn ?: "",
            rolle = rolle,
        )
    }

    fun hentSaksbehandlerInfo(navIdent: String): Saksbehandler = saksbehandlerClient.hentSaksbehandlerInfo(navIdent)

    private fun utledSaksbehandlerRolle(
        behandlingId: BehandlingId,
        oppgave: Oppgave?,
    ): SaksbehandlerRolle {
        if (erUtviklerMedVeilderrolle()) {
            return SaksbehandlerRolle.UTVIKLER_MED_VEILDERROLLE
        }

        if (oppgave == null) {
            val behandling = behandlingRepository.findByIdOrThrow(behandlingId)

            if (behandling.steg == StegType.INNGANGSVILKÅR ||
                behandling.steg == StegType.VILKÅR ||
                behandling.steg == StegType.BEREGNE_YTELSE
            ) {
                return SaksbehandlerRolle.OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER
            }
            return SaksbehandlerRolle.OPPGAVE_FINNES_IKKE
        }

        // Skal vi sjekke for både tso og tsr?
        if (oppgave.tema != Tema.TSO || oppgave.status == StatusEnum.FEILREGISTRERT) {
            return SaksbehandlerRolle.OPPGAVE_TILHØRER_IKKE_TILLEGGSSTONADER
        }

        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()
        return when (oppgave.tilordnetRessurs) {
            innloggetSaksbehandler -> SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER
            null -> SaksbehandlerRolle.IKKE_SATT
            else -> SaksbehandlerRolle.ANNEN_SAKSBEHANDLER
        }
    }

    private fun erUtviklerMedVeilderrolle(): Boolean {
        // Trenger vi dette?
        // val bryter = featureToggleService.isEnabled(Toggle.UTVIKLER_MED_VEILEDERRROLLE)
        return false
    }
}
