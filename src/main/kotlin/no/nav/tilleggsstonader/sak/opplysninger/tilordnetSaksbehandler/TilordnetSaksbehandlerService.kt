package no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler

import no.nav.tilleggsstonader.kontrakter.felles.Saksbehandler
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveRepository
import no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler.domain.TilordnetSaksbehandler
import no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler.domain.TilordnetSaksbehandlerPåOppgave
import org.springframework.stereotype.Service

@Service
class TilordnetSaksbehandlerService(
    private val oppgaveRepository: OppgaveRepository,
    private val behandlingRepository: BehandlingRepository,
    private val saksbehandlerClient: TilordnetSaksbehandlerClient,
) {
    fun finnTilordnetSaksbehandler(behandlingId: BehandlingId): TilordnetSaksbehandler {
        val oppgave = hentOppgaveMedTypeSomIkkeErFerdigstilt(behandlingId)
        return utledTilordnetSaksbehandlerForOppgave(behandlingId, oppgave)
    }

    private fun hentOppgaveMedTypeSomIkkeErFerdigstilt(behandlingId: BehandlingId): OppgaveDomain? {
        val oppgavetyper =
            setOf(
                Oppgavetype.BehandleSak,
                Oppgavetype.BehandleUnderkjentVedtak,
                Oppgavetype.GodkjenneVedtak,
            )
        return oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
            behandlingId,
            oppgavetyper,
        )
    }

    private fun utledTilordnetSaksbehandlerForOppgave(
        behandlingId: BehandlingId,
        behandleSakOppgave: OppgaveDomain?,
    ): TilordnetSaksbehandler {
        val tilordnetSaksbehandlerPåOppgave = utledSaksbehandlerRolle(behandlingId, behandleSakOppgave)

        val tilordnetSaksbehandler =
            if (tilordnetSaksbehandlerPåOppgave ==
                TilordnetSaksbehandlerPåOppgave.OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER
            ) {
                hentSaksbehandlerInfo(SikkerhetContext.hentSaksbehandler())
            } else if (behandleSakOppgave?.tilordnetSaksbehandler == null) {
                // Dersom oppgaven er null og tilordnet saksbehandler er null får man status SaksbehandlerRolle.OPPGAVE_FINNES_IKKE
                null
            } else {
                hentSaksbehandlerInfo(behandleSakOppgave.tilordnetSaksbehandler)
            }

        return TilordnetSaksbehandler(
            navIdent = tilordnetSaksbehandler?.navIdent,
            etternavn = tilordnetSaksbehandler?.etternavn,
            fornavn = tilordnetSaksbehandler?.fornavn,
            tilordnetSaksbehandlerPåOppgave = tilordnetSaksbehandlerPåOppgave,
        )
    }

    fun hentSaksbehandlerInfo(navIdent: String): Saksbehandler = saksbehandlerClient.hentSaksbehandlerInfo(navIdent)

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

    private fun utledSaksbehandlerRolle(
        behandlingId: BehandlingId,
        oppgave: OppgaveDomain?,
    ): TilordnetSaksbehandlerPåOppgave {
        if (oppgave == null) {
            val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
            if (behandling.steg == StegType.INNGANGSVILKÅR ||
                behandling.steg == StegType.VILKÅR ||
                behandling.steg == StegType.BEREGNE_YTELSE
            ) {
                return TilordnetSaksbehandlerPåOppgave.OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER
            }
            return TilordnetSaksbehandlerPåOppgave.OPPGAVE_FINNES_IKKE
        }

        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()
        return when (oppgave.tilordnetSaksbehandler) {
            innloggetSaksbehandler -> TilordnetSaksbehandlerPåOppgave.INNLOGGET_SAKSBEHANDLER
            null -> TilordnetSaksbehandlerPåOppgave.IKKE_SATT
            else -> TilordnetSaksbehandlerPåOppgave.ANNEN_SAKSBEHANDLER
        }
    }
}
