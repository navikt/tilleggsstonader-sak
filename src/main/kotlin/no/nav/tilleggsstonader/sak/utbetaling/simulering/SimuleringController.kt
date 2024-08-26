package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

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
    fun simulerForBehandling(@PathVariable behandlingId: UUID): SimuleringDto? {
        feilHvisIkke(unleashService.isEnabled(Toggle.SIMULERING)) {
            "Toggle for simulering er skrudd av"
        }

        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        tilgangService.validerTilgangTilBehandling(saksbehandling, AuditLoggerEvent.UPDATE)

        val perioder = simuleringStegService.hentEllerOpprettSimuleringsresultat(saksbehandling)

        return if (perioder.isNullOrEmpty()) {
            null
        } else {
            SimuleringDto(perioder = perioder)
        }
    }
}
