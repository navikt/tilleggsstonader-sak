package no.nav.tilleggsstonader.sak.brev.kjørelistebrev

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.FerdigstillBehandlingTask
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import org.springframework.stereotype.Service

@Service
class JournalførOgDistribuerKjørelisteBehandlingBrevSteg(
    private val taskService: TaskService,
) : BehandlingSteg<Void?> {
    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        taskService.save(FerdigstillBehandlingTask.opprettTask(saksbehandling))
    }

    override fun stegType(): StegType = StegType.JOURNALFØR_OG_DISTRIBUER_KJØRELISTEBREV
}
