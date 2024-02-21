package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.BrevService
import no.nav.tilleggsstonader.sak.brev.JournalførVedtaksbrevTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.FerdigstillOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtaksresultatService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import org.springframework.stereotype.Service

/**
 * Det er litt blandning av begrep, totrinnskontroll, beslutte steg, etc
 * Burde det hete totrinnskontroll?
 * Endepunkt?
 * Steget?
 *
 * BeslutteVedtakController
 *
 */

@Service
class BeslutteVedtakSteg(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val taskService: TaskService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vedtaksresultatService: VedtaksresultatService,
    private val brevService: BrevService,
    private val iverksettService: IverksettService
) : BehandlingSteg<BeslutteVedtakDto> {

    override fun validerSteg(saksbehandling: Saksbehandling) {
        brukerfeilHvis(saksbehandling.steg.kommerEtter(stegType())) {
            "Behandlingen er allerede besluttet. Status på behandling er '${saksbehandling.status.visningsnavn()}'"
        }
        feilHvis(saksbehandling.steg != stegType()) {
            "Behandling er i feil steg=${saksbehandling.steg}"
        }
    }

    override fun utførOgReturnerNesteSteg(saksbehandling: Saksbehandling, data: BeslutteVedtakDto): StegType {
        fagsakService.fagsakMedOppdatertPersonIdent(saksbehandling.fagsakId)
        val saksbehandler = totrinnskontrollService.lagreTotrinnskontrollOgReturnerSaksbehandler(saksbehandling, data)
        val oppgaveId = ferdigstillOppgave(saksbehandling)

        return if (data.godkjent) {
            oppdaterResultatPåBehandling(saksbehandling)
            // opprettTaskForBehandlingsstatistikk(saksbehandling.id, oppgaveId)
            brevService.lagEndeligBeslutterbrev(saksbehandling)
            opprettJournalførVedtaksbrevTask(saksbehandling)

            iverksettService.iverksettBehandlingFørsteGang(saksbehandling.id)

            StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV
        } else {
            opprettBehandleUnderkjentVedtakOppgave(saksbehandling, saksbehandler)
            StegType.SEND_TIL_BESLUTTER
        }
    }

    private fun opprettJournalførVedtaksbrevTask(saksbehandling: Saksbehandling) {
        taskService.save(JournalførVedtaksbrevTask.opprettTask(saksbehandling.id))
    }

    private fun oppdaterResultatPåBehandling(saksbehandling: Saksbehandling) {
        val behandlingId = saksbehandling.id
        val resultat = vedtaksresultatService.hentVedtaksresultat(saksbehandling)
        when (resultat) {
            TypeVedtak.INNVILGET -> {
                behandlingService.oppdaterResultatPåBehandling(
                    behandlingId,
                    BehandlingResultat.INNVILGET,
                )
            }
        }
    }

    private fun opprettBehandleUnderkjentVedtakOppgave(saksbehandling: Saksbehandling, navIdent: String) {
        taskService.save(
            OpprettOppgaveTask.opprettTask(
                behandlingId = saksbehandling.id,
                OpprettOppgave(
                    oppgavetype = Oppgavetype.BehandleUnderkjentVedtak,
                    tilordnetNavIdent = navIdent,
                ),
            ),
        )
    }

    private fun ferdigstillOppgave(saksbehandling: Saksbehandling): Long? {
        val oppgavetype = Oppgavetype.GodkjenneVedtak
        return oppgaveService.hentOppgaveSomIkkeErFerdigstilt(saksbehandling.id, oppgavetype)?.let {
            taskService.save(
                FerdigstillOppgaveTask.opprettTask(
                    behandlingId = saksbehandling.id,
                    oppgavetype = oppgavetype,
                    oppgaveId = it.gsakOppgaveId,
                ),
            )
            it.gsakOppgaveId
        }
    }

    override fun utførSteg(saksbehandling: Saksbehandling, data: BeslutteVedtakDto) {
        error("Bruker utførOgReturnerNesteSteg i stedet for utførSteg")
    }

    override fun stegType(): StegType = StegType.BESLUTTE_VEDTAK

    override fun settInnHistorikk(): Boolean = false
}
