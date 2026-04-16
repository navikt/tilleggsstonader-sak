package no.nav.tilleggsstonader.sak.privatbil

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.FerdigstillBehandlingTask
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.FerdigstillOppgaveTask
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import org.springframework.stereotype.Component

@Component
class FullførKjørelistebehandlingSteg(
    private val behandlingService: BehandlingService,
    private val iverksettService: IverksettService,
    private val taskService: TaskService,
    private val oppgaveService: OppgaveService,
) : BehandlingSteg<Void?> {
    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        brukerfeilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke fullføre kjørelistebehandling=${saksbehandling.id} fordi den har status ${saksbehandling.status.visningsnavn()}."
        }
        brukerfeilHvis(saksbehandling.type != BehandlingType.KJØRELISTE) {
            "Kan ikke fullføre behandling=${saksbehandling.id} fordi den ikke er en kjørelistebehandling."
        }

        behandlingService.oppdaterResultatPåBehandling(saksbehandling.id, BehandlingResultat.INNVILGET)
        behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)
        taskService.save(FerdigstillBehandlingTask.opprettTask(saksbehandling))
        ferdigstillOppgave(saksbehandling)
        iverksettService.iverksettBehandlingFørsteGang(saksbehandling.id)
    }

    private fun ferdigstillOppgave(saksbehandling: Saksbehandling): Long? {
        val oppgavetype = Oppgavetype.BehandleKjøreliste
        return oppgaveService.hentOppgaveDomainSomIkkeErFerdigstilt(saksbehandling.id, oppgavetype)?.let {
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

    override fun stegType() = StegType.FULLFØR_KJØRELISTE
}
