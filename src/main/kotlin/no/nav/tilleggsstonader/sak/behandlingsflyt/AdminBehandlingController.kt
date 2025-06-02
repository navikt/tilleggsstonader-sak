package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.libs.log.mdc.MDCConstants
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.util.EnvUtil
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors

@RestController
@RequestMapping("/api/behandling/admin/ferdigstill")
@ProtectedWithClaims(issuer = "azuread")
class AdminBehandlingController(
    private val behandlingService: BehandlingService,
    private val stegService: StegService,
    private val transactionHandler: TransactionHandler,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val behandlingIdProd = BehandlingId.fromString("4ed927ed-6d69-42b5-9391-137dd7c94962")

    @PostMapping("{behandlingId}")
    fun ferdigstillBehandling(
        @PathVariable behandlingId: BehandlingId,
    ) {
        feilHvis(!EnvUtil.erIDev() && behandlingId != behandlingIdProd) {
            "Kan kun ferdigstille behandling med id 4ed927ed-6d69-42b5-9391-137dd7c94962 i prod"
        }
        val callId = MDC.get(MDCConstants.MDC_CALL_ID)
        Executors.newVirtualThreadPerTaskExecutor().submit {
            MDC.put(MDCConstants.MDC_CALL_ID, callId)
            try {
                ferdigstill(behandlingId)
            } catch (e: Throwable) {
                logger.warn("Feil ved oppdatering av vedtak. Se securelogs")
                secureLogger.error("Feil ved oppdatering av vedtak", e)
            }
        }
    }

    fun ferdigstill(behandlingId: BehandlingId) {
        transactionHandler.runInNewTransaction {
            stegService.håndterSteg(behandlingId, StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV)
        }
    }
}
