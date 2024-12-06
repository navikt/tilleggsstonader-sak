package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.interntVedtak.InterntVedtakTask
import no.nav.tilleggsstonader.sak.statistikk.task.BehandlingsstatistikkTask
import no.nav.tilleggsstonader.sak.statistikk.vedtak.VedtaksstatistikkTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FerdigstillBehandlingSteg(
    private val behandlingService: BehandlingService,
    private val taskService: TaskService,
    private val unleashService: UnleashService,
) : BehandlingSteg<Void?> {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        logger.info("Ferdigstiller behandling=${saksbehandling.id}")
        behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.FERDIGSTILT)

        val taskVersjon = if (unleashService.isEnabled(Toggle.BRUK_INTERNT_VEDTAK_V2)) "2" else "1"
        taskService.save(InterntVedtakTask.lagTask(saksbehandling.id, taskVersjon))

        taskService.save(BehandlingsstatistikkTask.opprettFerdigTask(behandlingId = saksbehandling.id))

        taskService.save(
            VedtaksstatistikkTask.opprettVedtaksstatistikkTask(
                behandlingId = saksbehandling.id,
                fagsakId = saksbehandling.fagsakId,
                stønadstype = saksbehandling.stønadstype,
            ),
        )
    }

    override fun stegType(): StegType {
        return StegType.FERDIGSTILLE_BEHANDLING
    }
}
