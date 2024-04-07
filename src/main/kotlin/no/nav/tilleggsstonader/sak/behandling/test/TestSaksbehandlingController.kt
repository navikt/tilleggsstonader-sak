package no.nav.tilleggsstonader.sak.behandling.test

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/test")
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class TestSaksbehandlingController(
    private val testSaksbehandlingService: TestSaksbehandlingService,
) {

    @PostMapping("{behandlingId}/oppfyll-inngangsvilkar")
    fun utfyllInngangsvilkår(@PathVariable behandlingId: UUID): UUID {
        return testSaksbehandlingService.utfyllInngangsvilkår(behandlingId)
    }

    @PostMapping("{behandlingId}/oppfyll-vilkar")
    fun utfyllVilkår(@PathVariable behandlingId: UUID): UUID {
        return testSaksbehandlingService.utfyllVilkår(behandlingId)
    }
}
