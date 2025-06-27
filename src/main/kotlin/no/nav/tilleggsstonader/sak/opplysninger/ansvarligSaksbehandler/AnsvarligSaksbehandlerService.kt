package no.nav.tilleggsstonader.sak.opplysninger.ansvarligSaksbehandler

import no.nav.tilleggsstonader.kontrakter.felles.Saksbehandler
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.ansvarligSaksbehandler.domain.AnsvarligSaksbehandler
import no.nav.tilleggsstonader.sak.opplysninger.ansvarligSaksbehandler.domain.SaksbehandlerRolle
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveRepository
import org.springframework.stereotype.Service

@Service
class AnsvarligSaksbehandlerService(
    private val oppgaveRepository: OppgaveRepository,
    private val behandlingRepository: BehandlingRepository,
    private val saksbehandlerClient: AnsvarligSaksbehandlerClient,
) {
    /**
     *  I tilfeller hvor saksbehandler manuelt oppretter en revurdering eller en førstegangsbehandling vil oppgaven
     *  som returneres fra oppgavesystemet være null. Dette skjer fordi oppgavesystemet bruker litt tid av variabel
     *  lengde på å opprette den tilhørende behandle-sak-oppgaven til den opprettede behandlingen.
     *
     * [SaksbehandlerRolle.OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER]: Dersom null blir returnert og
     * behandlingen befinner seg i steget REVURDERING_ÅRSAK, VILKÅR eller BEREGNE_YTELSE anser vi det som svært
     * sannsynlig at det er den innloggede saksbehandleren som er ansvarlig for behandlingen - oppgaven har bare ikke
     * rukket å bli opprettet enda. I dette tilfellet returnerer vi OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER
     * til frontend.
     *
     * [SaksbehandlerRolle.OPPGAVE_FINNES_IKKE]: Dersom null returneres og behandlingen ikke befinner seg i et av de nevnte
     * stegene returnerer vi OPPGAVE_FINNES_IKKE til frontend.
     */

    fun finnAnsvarligSaksbehandler(behandlingId: BehandlingId): AnsvarligSaksbehandler {
        val oppgave = hentIkkeFerdigstiltOppgaveForBehandling(behandlingId)
        return utledAnsvarligSaksbehandlerForOppgave(behandlingId, oppgave)
    }

    private fun hentIkkeFerdigstiltOppgaveForBehandling(behandlingId: BehandlingId): OppgaveDomain? {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val oppgavetyper =
            when (behandling.steg.tillattFor) {
                BehandlerRolle.SAKSBEHANDLER -> setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak)
                BehandlerRolle.BESLUTTER -> setOf(Oppgavetype.GodkjenneVedtak)
                else -> emptySet()
            }
        return hentOppgaveMedTypeSomIkkeErFerdigstilt(behandlingId, oppgavetyper)
    }

    private fun hentOppgaveMedTypeSomIkkeErFerdigstilt(
        behandlingId: BehandlingId,
        oppgavetyper: Set<Oppgavetype>,
    ): OppgaveDomain? =
        oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
            behandlingId,
            oppgavetyper,
        )

    private fun utledAnsvarligSaksbehandlerForOppgave(
        behandlingId: BehandlingId,
        behandleSakOppgave: OppgaveDomain?,
    ): AnsvarligSaksbehandler {
        val rolle = utledSaksbehandlerRolle(behandlingId, behandleSakOppgave)

        val tilordnetSaksbehandler =
            if (rolle == SaksbehandlerRolle.OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER) {
                hentSaksbehandlerInfo(SikkerhetContext.hentSaksbehandler())
            } else if (behandleSakOppgave?.tilordnetSaksbehandler == null) {
                // Dersom oppgaven er null og tilordnet saksbehandler er null får man status SaksbehandlerRolle.OPPGAVE_FINNES_IKKE
                null
            } else {
                hentSaksbehandlerInfo(behandleSakOppgave.tilordnetSaksbehandler)
            }

        return AnsvarligSaksbehandler(
            etternavn = tilordnetSaksbehandler?.etternavn,
            fornavn = tilordnetSaksbehandler?.fornavn,
            rolle = rolle,
        )
    }

    fun hentSaksbehandlerInfo(navIdent: String): Saksbehandler = saksbehandlerClient.hentSaksbehandlerInfo(navIdent)

    private fun utledSaksbehandlerRolle(
        behandlingId: BehandlingId,
        oppgave: OppgaveDomain?,
    ): SaksbehandlerRolle {
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

        // todo - høre om dette er nødvendig

//        val t = oppgaveClient.finnOppgaveMedId(oppgave.gsakOppgaveId)
//
//         if (t.tema != Tema.TSO || t.tema != Tema.TSR || t.status == StatusEnum.FEILREGISTRERT) {
//            return SaksbehandlerRolle.OPPGAVE_TILHØRER_IKKE_TILLEGGSSTONADER
//        }

        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()
        return when (oppgave.tilordnetSaksbehandler) {
            innloggetSaksbehandler -> SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER
            null -> SaksbehandlerRolle.IKKE_SATT
            else -> SaksbehandlerRolle.ANNEN_SAKSBEHANDLER
        }
    }
}
