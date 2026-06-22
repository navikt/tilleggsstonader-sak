package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakDtoMapper
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.ReiseTilSamlingBeregningService
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.BeregningsresultatReiseTilSamlingDto
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.InnvilgelseReiseTilSamlingTsoRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedtak/reise-til-samling")
@ProtectedWithClaims(issuer = "azuread")
class ReiseTilSamlingVedtakController(
    private val behandlingService: BehandlingService,
    private val tilgangService: TilgangService,
    private val vedtakService: VedtakService,
    private val vedtakDtoMapper: VedtakDtoMapper,
    private val beregningService: ReiseTilSamlingBeregningService,
) {
    @GetMapping("{behandlingId}")
    fun hentVedtak(
        @PathVariable behandlingId: BehandlingId,
    ): VedtakResponse? {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerLesetilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val vedtak = vedtakService.hentVedtak(behandlingId) ?: return null
        return vedtakDtoMapper.toDto(vedtak, behandling.forrigeIverksatteBehandlingId)
    }

    @PostMapping("{behandlingId}/tso/beregn")
    fun beregn(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseReiseTilSamlingTsoRequest,
    ): BeregningsresultatReiseTilSamlingDto = beregnVedtak(behandlingId, vedtak.vedtaksperioder())

    private fun beregnVedtak(
        behandlingId: BehandlingId,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatReiseTilSamlingDto {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val beregningsresultat =
            beregningService.beregn(
                behandling = behandling,
                vedtaksperioder = vedtaksperioder,
                typeVedtak = TypeVedtak.INNVILGELSE,
            )

        return beregningsresultat.tilDto
    }
}
