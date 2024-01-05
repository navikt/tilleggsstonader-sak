package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.VedtaksbrevRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.FerdigstillOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtaksresultatService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.TotrinnkontrollStatus
import no.nav.tilleggsstonader.sak.vilkår.VilkårService
import org.springframework.stereotype.Service

@Service
class SendTilBeslutterSteg(
    private val taskService: TaskService,
    private val behandlingService: BehandlingService,
    private val vedtaksbrevRepository: VedtaksbrevRepository,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val vedtaksresultatService: VedtaksresultatService,
    private val vilkårService: VilkårService,
    private val oppgaveService: OppgaveService,
    private val totrinnskontrollService: TotrinnskontrollService,
) : BehandlingSteg<Void?> {

    // TODO valider at man har opprettet vedtaksbrev?
    override fun validerSteg(saksbehandling: Saksbehandling) {
        brukerfeilHvis(saksbehandling.steg != stegType()) {
            "Behandling er i feil steg=${saksbehandling.steg}"
        }
        /* TODO feilutbetaling tilbakekreving
        brukerfeilHvis(saksbehandlerMåTaStilingTilTilbakekreving(saksbehandling)) {
            "Feilutbetaling detektert. Må ta stilling til feilutbetalingsvarsel under simulering"
        }
         */
        validerRiktigTilstandVedInvilgelse(saksbehandling)
        validerSaksbehandlersignatur(saksbehandling)
        validerAtDetFinnesOppgave(saksbehandling)
    }

    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.FATTER_VEDTAK)
        totrinnskontrollService.sendtilBeslutter(saksbehandling)
        opprettGodkjennVedtakOppgave(saksbehandling)
        ferdigstillOppgave(saksbehandling)
        // opprettTaskForBehandlingsstatistikk(saksbehandling.id) TODO DVH
    }

    private fun opprettGodkjennVedtakOppgave(saksbehandling: Saksbehandling) {
        taskService.save(
            OpprettOppgaveTask.opprettTask(
                behandlingId = saksbehandling.id,
                oppgave = OpprettOppgave(
                    oppgavetype = Oppgavetype.GodkjenneVedtak,
                    beskrivelse = "Sendt til godkjenning av ${SikkerhetContext.hentSaksbehandlerNavn(true)}.",
                ),
            ),
        )
    }

    private fun ferdigstillOppgave(saksbehandling: Saksbehandling) {
        val totrinnskontrollServiceStatus = totrinnskontrollService.hentTotrinnskontrollStatus(saksbehandling.id)
        val oppgavetype = if (totrinnskontrollServiceStatus.status == TotrinnkontrollStatus.TOTRINNSKONTROLL_UNDERKJENT) {
            Oppgavetype.BehandleUnderkjentVedtak
        } else {
            Oppgavetype.BehandleSak
        }

        taskService.save(
            FerdigstillOppgaveTask.opprettTask(
                behandlingId = saksbehandling.id,
                oppgavetype,
                null,
            ),
        )
    }

    private fun validerRiktigTilstandVedInvilgelse(saksbehandling: Saksbehandling) {
        val vedtaksresultat = vedtaksresultatService.hentVedtaksresultat(saksbehandling)
        if (vedtaksresultat == TypeVedtak.INNVILGET) {
            brukerfeilHvisIkke(vilkårService.erAlleVilkårOppfylt(saksbehandling.id)) {
                "Kan ikke innvilge hvis ikke alle vilkår er oppfylt for behandlingId: ${saksbehandling.id}"
            }
        }
    }

    private fun validerSaksbehandlersignatur(saksbehandling: Saksbehandling) {
        if (saksbehandling.skalIkkeSendeBrev) return

        val vedtaksbrev = vedtaksbrevRepository.findByIdOrThrow(saksbehandling.id)

        brukerfeilHvis(vedtaksbrev.saksbehandlerIdent != SikkerhetContext.hentSaksbehandler()) {
            "En annen saksbehandler har signert vedtaksbrevet"
        }
    }

    private fun validerAtDetFinnesOppgave(saksbehandling: Saksbehandling) {
        feilHvis(oppgaveService.hentBehandleSakOppgaveSomIkkeErFerdigstilt(saksbehandling.id) == null) {
            "Oppgaven for behandlingen er ikke tilgjengelig. Vennligst vent og prøv igjen om litt."
        }
    }

    override fun stegType(): StegType = StegType.SEND_TIL_BESLUTTER
}
