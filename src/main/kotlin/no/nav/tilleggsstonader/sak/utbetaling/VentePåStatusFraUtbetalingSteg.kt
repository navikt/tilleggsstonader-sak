package no.nav.tilleggsstonader.sak.utbetaling

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.JournalførVedtaksbrevTask
import org.springframework.stereotype.Service

@Service
class VentePåStatusFraUtbetalingSteg(
    private val taskService: TaskService,
) : BehandlingSteg<Void?> {

    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        // TODO sjekk status

        taskService.save(JournalførVedtaksbrevTask.opprettTask(saksbehandling.id))
    }

    override fun stegType(): StegType {
        return StegType.VENTE_PÅ_STATUS_FRA_UTBETALING
    }
}
