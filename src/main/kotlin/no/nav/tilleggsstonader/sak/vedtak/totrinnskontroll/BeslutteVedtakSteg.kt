package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.FerdigstillBehandlingTask
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.vedtaksbrev.BrevService
import no.nav.tilleggsstonader.sak.brev.vedtaksbrev.JournalførVedtaksbrevTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.FerdigstillOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtaksresultatService
import no.nav.tilleggsstonader.sak.vedtak.tilBehandlingResult
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import org.springframework.stereotype.Service
import java.time.LocalDate

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
    private val iverksettService: IverksettService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val unleashService: UnleashService,
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
            validerIkkeGjørEndringerPåTidligereUtbetalinger(saksbehandling)
            // opprettTaskForBehandlingsstatistikk(saksbehandling.id, oppgaveId)

            iverksettService.iverksettBehandlingFørsteGang(saksbehandling.id)
            if (!saksbehandling.skalIkkeSendeBrev) {
                brevService.lagEndeligBeslutterbrev(saksbehandling)
                opprettJournalførVedtaksbrevTask(saksbehandling)
                StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV
            } else {
                taskService.save(FerdigstillBehandlingTask.opprettTask(saksbehandling))
                StegType.FERDIGSTILLE_BEHANDLING
            }
        } else {
            opprettBehandleUnderkjentVedtakOppgave(saksbehandling, saksbehandler)
            StegType.SEND_TIL_BESLUTTER
        }
    }

    private fun validerIkkeGjørEndringerPåTidligereUtbetalinger(saksbehandling: Saksbehandling) {
        if (unleashService.isEnabled(Toggle.SPESIAL_IVERKSETT_ENDRINGER)) {
            return
        }

        val forrigeBehandlingId = saksbehandling.forrigeBehandlingId
        if (
            forrigeBehandlingId == null ||
            vedtaksresultatService.hentVedtaksresultat(saksbehandling) == TypeVedtak.AVSLAG
        ) {
            return
        }
        val forrigeAndeler = andelerFraForrigeBehandlingSomErIverksatte(forrigeBehandlingId)
        val forrigeMaksTom = forrigeAndeler.maxOfOrNull { it.tom } ?: return

        val andeler = tilkjentYtelseService.hentForBehandling(saksbehandling.id).andelerTilkjentYtelse
            .tilForenkledeAndeler()
            .filter { it.fom <= forrigeMaksTom }
            .sorted()

        feilHvis(forrigeAndeler != andeler) {
            secureLogger.info("Endringer i andeler=$andeler forrigeAndeler=$forrigeAndeler")
            "Denne iverksettingen blir kanskje endringer på perioder som allerede er utbetalt. " +
                "Det har vi ennå ikke støtte for og kan foreløpig ikke godkjennes."
        }
    }

    private fun andelerFraForrigeBehandlingSomErIverksatte(forrigeBehandlingId: BehandlingId) =
        tilkjentYtelseService.hentForBehandling(forrigeBehandlingId).andelerTilkjentYtelse
            .filter {
                it.statusIverksetting in setOf(
                    StatusIverksetting.SENDT,
                    StatusIverksetting.OK,
                    StatusIverksetting.OK_UTEN_UTBETALING,
                )
            }
            .tilForenkledeAndeler()
            .sorted()

    private fun Collection<AndelTilkjentYtelse>.tilForenkledeAndeler() =
        this.map { ForenkletAndel(it.fom, it.tom, it.beløp, it.type) }.toSet()

    private data class ForenkletAndel(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val beløp: Int,
        val type: TypeAndel,
    ) : Periode<LocalDate>

    private fun opprettJournalførVedtaksbrevTask(saksbehandling: Saksbehandling) {
        taskService.save(JournalførVedtaksbrevTask.opprettTask(saksbehandling.id))
    }

    private fun oppdaterResultatPåBehandling(saksbehandling: Saksbehandling) {
        val behandlingId = saksbehandling.id
        val resultat = vedtaksresultatService.hentVedtaksresultat(saksbehandling)
        validerIkkeGjørEndringerPåTidligereUtbetalinger(saksbehandling)
        behandlingService.oppdaterResultatPåBehandling(
            behandlingId = behandlingId,
            behandlingResultat = resultat.tilBehandlingResult(),
        )
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
