package no.nav.tilleggsstonader.sak.utbetaling.migrering

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors

@Tag(name = "Forvaltning")
@RestController
@RequestMapping("/api/forvaltning/migrer-utbetalinger")
@ProtectedWithClaims(issuer = "azuread")
class FagsakUtbetalingIdMigreringController(
    private val fagsakUtbetalingIdMigreringService: FagsakUtbetalingIdMigreringService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    fun migrerFagsak(fagsakId: FagsakId) {
        logger.info("Migrerer fagsak $fagsakId")
        val prosess =
            Executors.newVirtualThreadPerTaskExecutor().submit {
                fagsakUtbetalingIdMigreringService.migrerForFagsak(fagsakId)
            }
    }
}
