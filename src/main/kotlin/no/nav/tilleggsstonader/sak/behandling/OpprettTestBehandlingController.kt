package no.nav.tilleggsstonader.sak.behandling

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping(path = ["/api/test/opprett-behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class OpprettTestBehandlingController(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
) {

    @Transactional
    @PostMapping
    fun opprettBehandling(@RequestBody testBehandlingRequest: TestBehandlingRequest): UUID {
        val fagsak: Fagsak = lagFagsak(testBehandlingRequest)
        val behandling: Behandling = lagBehandling(fagsak)

        return behandling.id
    }

    private fun lagBehandling(fagsak: Fagsak) = behandlingService.opprettBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING, fagsakId = fagsak.id, behandlingsårsak = BehandlingÅrsak.SØKNAD)

    private fun lagFagsak(testBehandlingRequest: TestBehandlingRequest) = fagsakService.hentEllerOpprettFagsak(testBehandlingRequest.personIdent, Stønadstype.BARNETILSYN)
}

data class TestBehandlingRequest(
    val personIdent: String,
    val stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
)
