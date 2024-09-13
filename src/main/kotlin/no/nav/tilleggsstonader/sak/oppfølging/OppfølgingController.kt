package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping(path = ["/api/oppfoelging"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppfølgingController(
    private val tilgangService: TilgangService,
    private val oppfølgingService: OppfølgingService,
    private val unleashService: UnleashService,
) {

    @GetMapping("behandlinger")
    fun hentBehandlingerForOppfølging(): List<BehandlingForOppfølgingDto> {
        tilgangService.validerTilgangTilRolle(BehandlerRolle.VEILEDER)

        feilHvisIkke(unleashService.isEnabled(Toggle.HENT_BEHANDLINGER_FOR_OPPFØLGING)) {
            "Feature toggle ${Toggle.HENT_BEHANDLINGER_FOR_OPPFØLGING} er ikke aktivert"
        }

        return oppfølgingService.hentBehandlingerForOppfølging()
    }
}
