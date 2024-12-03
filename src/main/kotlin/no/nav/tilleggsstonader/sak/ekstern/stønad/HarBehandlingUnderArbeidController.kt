package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/ekstern/har-behandling"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class HarBehandlingUnderArbeidController(
    private val fagsakService: FagsakService,
) {
    @PostMapping()
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun hentBehandlingStatusForPersonMedStønadstype(@RequestBody identStønadstype: IdentStønadstype): Boolean {
        val behandlinger = fagsakService.hentBehandlingerForPersonOgStønadstype(
            identStønadstype.ident,
            identStønadstype.stønadstype,
        )
        if (behandlinger.isNotEmpty()) {
            val validStatuses = listOf(
                BehandlingStatus.OPPRETTET,
                BehandlingStatus.UTREDES,
                BehandlingStatus.FATTER_VEDTAK,
                BehandlingStatus.SATT_PÅ_VENT,
            )
            val behandlingStatus = behandlinger.map { it.status }
            return behandlingStatus.any { it in validStatuses }
        }
        return false
    }
}
