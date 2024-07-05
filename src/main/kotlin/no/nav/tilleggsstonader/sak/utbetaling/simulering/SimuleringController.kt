package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/simulering"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SimuleringController(
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val simuleringService: SimuleringService,
) {

    @GetMapping("/{behandlingId}")
    fun simulerForBehandling(@PathVariable behandlingId: UUID): SimuleringDto {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        tilgangService.validerTilgangTilBehandling(saksbehandling, AuditLoggerEvent.UPDATE)
        return SimuleringDto(
            perioder = simuleringService.simuler(saksbehandling),
            // Mocker oppsummering da det ikke er bestemt om vi eller utsjekk skal lage
            oppsummering = SimuleringOppsummering(
                fom = LocalDate.of(2023, 7, 1),
                tom = LocalDate.of(2024, 7, 31),
                etterbetaling = 0,
                feilutbetaling = 2724,
                nesteUtbetaling = null,
            )
        )
    }
}

