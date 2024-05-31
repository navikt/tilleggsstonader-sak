package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@ProtectedWithClaims(issuer = "azuread")
@RequestMapping("/api/vedtaksstatistikk")
@Validated
class OpprettVedtaksstatistikkController(
    private val behandlingService: BehandlingService,
    private val taskService: TaskService,
) {

    @PostMapping
    fun opprettVedtaksstatistikk() {
        val behandlingId = UUID.fromString("a73f0a65-0a92-4840-b1af-f7d229057efa")
        val behandling = behandlingService.hentSaksbehandling(behandlingId)

        taskService.save(
            VedtaksstatistikkTask.opprettVedtaksstatistikkTask(
                behandlingId = behandlingId,
                fagsakId = behandling.fagsakId,
                stønadstype = behandling.stønadstype,
            ),
        )
    }
}
