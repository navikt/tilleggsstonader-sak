package no.nav.tilleggsstonader.sak.vedtak

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.SluttdatoForVedtakDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilLagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.forslag.ForeslåVedtaksperiodeService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedtak")
@ProtectedWithClaims(issuer = "azuread")
class VedtakController(
    private val tilgangService: TilgangService,
    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
    private val foreslåVedtaksperiodeService: ForeslåVedtaksperiodeService,
) {
    @GetMapping("{behandlingId}/sluttdato")
    fun hentSluttdatoForVedtakPåBehandling(
        @PathVariable behandlingId: BehandlingId,
    ): SluttdatoForVedtakDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)

        return SluttdatoForVedtakDto(
            sluttdato = vedtakService.hentVedtak(behandlingId)?.vedtaksperioderHvisFinnes()?.maxOfOrNull { it.tom },
        )
    }

    @GetMapping("{behandlingId}/foresla")
    fun foreslåVedtaksperioder(
        @PathVariable behandlingId: BehandlingId,
    ): List<LagretVedtaksperiodeDto> {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarSaksbehandlerrolle()

        val behandling = behandlingService.hentBehandling(behandlingId)
        val forrigeVedtaksperioder =
            behandling.forrigeIverksatteBehandlingId?.let {
                vedtakService.hentVedtaksperioder(it)
            }

        return foreslåVedtaksperiodeService.foreslåPerioder(behandlingId).tilLagretVedtaksperiodeDto(
            tidligereVedtaksperioder = forrigeVedtaksperioder,
        )
    }
}
