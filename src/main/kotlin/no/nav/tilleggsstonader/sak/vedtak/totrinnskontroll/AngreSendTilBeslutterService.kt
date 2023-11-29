package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.FerdigstillOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AngreSendTilBeslutterService(
    private val oppgaveService: OppgaveService,
    private val behandlingService: BehandlingService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val taskService: TaskService,
    private val totrinnskontrollService: TotrinnskontrollService,
) {

    @Transactional
    fun angreSendTilBeslutter(behandlingId: UUID) {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        validerKanAngreSendTilBeslutter(saksbehandling)
        totrinnskontrollService.angreSendTilBeslutter(behandlingId)
        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingId = behandlingId,
            stegtype = saksbehandling.steg,
            utfall = StegUtfall.ANGRE_SEND_TIL_BESLUTTER,
            metadata = null,
        )

        ferdigstillGodkjenneVedtakOppgave(saksbehandling)
        opprettBehandleSakOppgave(saksbehandling)

        behandlingService.oppdaterStegPåBehandling(behandlingId, StegType.SEND_TIL_BESLUTTER)
        behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.UTREDES)
    }

    private fun opprettBehandleSakOppgave(saksbehandling: Saksbehandling) {
        taskService.save(
            OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTask.OpprettOppgaveTaskData(
                    behandlingId = saksbehandling.id,
                    oppgavetype = Oppgavetype.BehandleSak,
                    beskrivelse = "Angret send til beslutter",
                    tilordnetNavIdent = SikkerhetContext.hentSaksbehandler(),
                ),
            ),
        )
    }

    private fun ferdigstillGodkjenneVedtakOppgave(saksbehandling: Saksbehandling) {
        oppgaveService.hentOppgaveSomIkkeErFerdigstilt(saksbehandling.id, Oppgavetype.GodkjenneVedtak)?.let {
            taskService.save(
                FerdigstillOppgaveTask.opprettTask(
                    behandlingId = saksbehandling.id,
                    oppgavetype = Oppgavetype.GodkjenneVedtak,
                    it.gsakOppgaveId,
                ),
            )
        }
    }

    private fun validerKanAngreSendTilBeslutter(saksbehandling: Saksbehandling) {
        validerSaksbehandlerSomAngrer(saksbehandling)
        validerSteg(saksbehandling)
        validerStatus(saksbehandling)
        validerOppgave(saksbehandling)
    }

    private fun validerSaksbehandlerSomAngrer(saksbehandling: Saksbehandling) {
        val saksbehandlerSendtTilBeslutter =
            totrinnskontrollService.hentSaksbehandlerSomSendteTilBeslutter(saksbehandling.id)

        brukerfeilHvis(saksbehandlerSendtTilBeslutter != SikkerhetContext.hentSaksbehandler()) {
            "Kan kun angre send til beslutter dersom du er saksbehandler på vedtaket"
        }
    }

    private fun validerSteg(saksbehandling: Saksbehandling) {
        val beslutter = totrinnskontrollService.hentBeslutter(saksbehandling.id)
        feilHvis(saksbehandling.steg != StegType.BESLUTTE_VEDTAK, httpStatus = HttpStatus.BAD_REQUEST) {
            if (saksbehandling.steg.kommerEtter(StegType.BESLUTTE_VEDTAK)) {
                "Kan ikke angre send til beslutter da vedtaket er godkjent av $beslutter"
            } else {
                "Kan ikke angre send til beslutter når behandling er i steg ${saksbehandling.steg}"
            }
        }
    }

    private fun validerStatus(saksbehandling: Saksbehandling) {
        feilHvis(saksbehandling.status != BehandlingStatus.FATTER_VEDTAK, httpStatus = HttpStatus.BAD_REQUEST) {
            "Kan ikke angre send til beslutter når behandlingen har status ${saksbehandling.status}"
        }
    }

    private fun validerOppgave(
        saksbehandling: Saksbehandling,
    ) {
        val oppgave = oppgaveService.hentOppgaveSomIkkeErFerdigstilt(
            behandlingId = saksbehandling.id,
            oppgavetype = Oppgavetype.GodkjenneVedtak,
        ) ?: throw ApiFeil(
            feil = "Systemet har ikke rukket å opprette godkjenne vedtak oppgaven enda. Prøv igjen om litt.",
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
        )

        val tilordnetRessurs = oppgaveService.hentOppgave(oppgave.gsakOppgaveId).tilordnetRessurs
        brukerfeilHvis(tilordnetRessurs != null && tilordnetRessurs != SikkerhetContext.hentSaksbehandler()) {
            "Kan ikke angre send til beslutter når oppgave er plukket av $tilordnetRessurs"
        }
    }
}
