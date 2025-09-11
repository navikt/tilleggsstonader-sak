package no.nav.tilleggsstonader.sak.satsjustering

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Forvaltning")
@RestController
@RequestMapping("/api/forvaltning")
@ProtectedWithClaims(issuer = "azuread")
class SatsjusteringController(
    private val tilgangService: TilgangService,
    private val satsjusteringService: SatsjusteringService,
) {
    @PostMapping("/satsjustering/{stønadstype}")
    @Transactional
    fun kjørSatsjusteringForStønadstype(
        @PathVariable stønadstype: Stønadstype,
    ): List<BehandlingId> {
        tilgangService.validerHarUtviklerrolle()
        return satsjusteringService.opprettTaskerForBehandlingerSomKanSatsjusteres(stønadstype)
    }
}
