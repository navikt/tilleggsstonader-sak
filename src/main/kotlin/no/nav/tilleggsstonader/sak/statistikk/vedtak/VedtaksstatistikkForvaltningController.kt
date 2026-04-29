package no.nav.tilleggsstonader.sak.statistikk.vedtak

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.log.logger
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Forvaltning")
@RestController
@RequestMapping("/api/forvaltning/vedtaksstatistikk")
@ProtectedWithClaims(issuer = "azuread")
class VedtaksstatistikkForvaltningController(
    private val tilgangService: TilgangService,
    private val vedtaksstatistikkService: VedtaksstatistikkService,
) {
    @PostMapping("/oppdater/{behandlingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun oppdaterVedtaksstatistikk(
        @PathVariable behandlingId: BehandlingId,
    ) {
        tilgangService.validerHarUtviklerrolle()
        logger.info("Oppdaterer vedtaksstatistikk for behandling $behandlingId")
        vedtaksstatistikkService.oppdaterVedtaksstatistikkV2(behandlingId)
    }
}
