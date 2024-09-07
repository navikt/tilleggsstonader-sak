package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/simulering"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SimuleringController(
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val unleashService: UnleashService,
    private val simuleringStegService: SimuleringStegService,
) {

    @GetMapping("/{behandlingId}")
    fun simulerForBehandling(@PathVariable behandlingId: BehandlingId): SimuleringDto? {
        feilHvisIkke(unleashService.isEnabled(Toggle.SIMULERING)) {
            "Toggle for simulering er skrudd av"
        }

        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        tilgangService.validerTilgangTilBehandling(saksbehandling, AuditLoggerEvent.UPDATE)

        val simulering = simuleringStegService.hentEllerOpprettSimuleringsresultat(saksbehandling)

        return simulering?.let {
            SimuleringDto(
                perioder = simulering.data?.oppsummeringer,
                ingenEndringIUtbetaling = it.ingenEndringIUtbetaling,
            )
        }
    }
}
