package no.nav.tilleggsstonader.sak.oppfølging

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Forvaltning")
@RestController
@RequestMapping("/api/forvaltning/oppfolging")
@ProtectedWithClaims(issuer = "azuread")
class OppfølgingForvaltningController(
    private val tilgangService: TilgangService,
    private val oppfølgingOpprettKontrollerService: OppfølgingOpprettKontrollerService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/start/{tema}")
    fun startJobb(
        @PathVariable tema: Tema,
    ) {
        tilgangService.validerHarUtviklerrolle()
        try {
            oppfølgingOpprettKontrollerService.opprettTaskerForOppfølging(tema)
        } catch (e: Exception) {
            logger.warn("Feilet start av oppfølgingjobb, se secure logs for flere detaljer")
            secureLogger.error("Feilet start av oppfølgingjobb", e)
        }
    }
}
