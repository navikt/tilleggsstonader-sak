package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.vedtaksbrev.VedtaksbrevRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.FerdigstillOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtaksresultatService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.SendTilBeslutterRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import org.springframework.stereotype.Service

@Service
class SendTilBeslutterSteg(
    private val taskService: TaskService,
    private val behandlingService: BehandlingService,
    private val vedtaksbrevRepository: VedtaksbrevRepository,
    private val vedtaksresultatService: VedtaksresultatService,
    private val vilkårService: VilkårService,
    private val oppgaveService: OppgaveService,
    private val totrinnskontrollService: TotrinnskontrollService,
) : BehandlingSteg<SendTilBeslutterRequest> {
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
        validerOppgaver(saksbehandling)
    }

    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: SendTilBeslutterRequest,
    ) {
        behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.FATTER_VEDTAK)
        ferdigstillOppgave(saksbehandling)
        totrinnskontrollService.sendtilBeslutter(saksbehandling, data)
        opprettGodkjennVedtakOppgave(saksbehandling)
    }

    private fun opprettGodkjennVedtakOppgave(saksbehandling: Saksbehandling) {
        taskService.save(
            OpprettOppgaveTask.opprettTask(
                behandlingId = saksbehandling.id,
                oppgave =
                    OpprettOppgave(
                        oppgavetype = Oppgavetype.GodkjenneVedtak,
                        beskrivelse = "Sendt til godkjenning av ${SikkerhetContext.hentSaksbehandlerNavn(true)}.",
                    ),
            ),
        )
    }

    private fun ferdigstillOppgave(saksbehandling: Saksbehandling) {
        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontroll(saksbehandling.id)
        val oppgavetype =
            if (totrinnskontroll?.status == TotrinnInternStatus.UNDERKJENT) {
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
        if (vedtaksresultat == TypeVedtak.INNVILGELSE) {
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

    private fun validerOppgaver(saksbehandling: Saksbehandling) {
        val behandleSakOppgave = oppgaveService.hentBehandleSakOppgaveSomIkkeErFerdigstilt(saksbehandling.id)
        brukerfeilHvis(behandleSakOppgave == null) {
            "Oppgaven for behandlingen er ikke tilgjengelig. Prøv igjen om litt."
        }
        validerAtOppgaveIkkeErPlukketAvAnnenSaksbehandler(behandleSakOppgave)

        brukerfeilHvis(
            oppgaveService.hentOppgaveSomIkkeErFerdigstilt(
                saksbehandling.id,
                Oppgavetype.GodkjenneVedtak,
            ) != null,
        ) {
            "Det finnes en Godkjenne Vedtak oppgave systemet må ferdigstille før behandlingen kan sendes til beslutter. Prøv igjen om litt"
        }
    }

    private fun validerAtOppgaveIkkeErPlukketAvAnnenSaksbehandler(behandleSakOppgave: OppgaveDomain) {
        val tilordnetRessurs = oppgaveService.hentOppgave(behandleSakOppgave.gsakOppgaveId).tilordnetRessurs
        brukerfeilHvis(tilordnetRessurs != null && tilordnetRessurs != SikkerhetContext.hentSaksbehandler()) {
            "Kan ikke sende til beslutter. Oppgaven for behandlingen er plukket av $tilordnetRessurs"
        }
    }

    override fun stegType(): StegType = StegType.SEND_TIL_BESLUTTER

    override fun settInnHistorikk(): Boolean = false
}
