package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegFerdigstiltResponse
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.behandlingsflyt.tilStegFerdigstiltResponse
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.BeregningsplanUtleder
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakDtoMapper
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.ReiseTilSamlingBeregningService
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.BeregningsresultatReiseTilSamlingDto
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.InnvilgelseReiseTilSamlingTsoRequest
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.VedtakReiseTilSamlingRequest
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.tilDto
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
    private val stegService: StegService,
    private val beregnYtelseSteg: ReiseTilSamlingBeregnYtelseSteg,
    private val beregningsplanUtleder: BeregningsplanUtleder,
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
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val plan =
            beregningsplanUtleder.utledForInnvilgelse(
                saksbehandling = behandling,
                vedtaksperioder = vedtaksperioder,
            )
        val beregningsresultat =
            beregningService.beregn(
                behandling = behandling,
                vedtaksperioder = vedtaksperioder,
                typeVedtak = TypeVedtak.INNVILGELSE,
                beregningsplan = plan,
            )
        return beregningsresultat.tilDto(beregningsplan = plan)
    }

    @PostMapping("{behandlingId}/tso/innvilgelse")
    fun innvilge(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseReiseTilSamlingTsoRequest,
    ): StegFerdigstiltResponse = lagreVedtak(behandlingId, vedtak).tilStegFerdigstiltResponse()

    private fun lagreVedtak(
        behandlingId: BehandlingId,
        vedtak: VedtakReiseTilSamlingRequest,
    ): Behandling {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)

        return stegService.håndterSteg(behandlingId, beregnYtelseSteg, vedtak)
    }
}
