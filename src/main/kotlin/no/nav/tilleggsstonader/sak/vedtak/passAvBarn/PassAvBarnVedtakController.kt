package no.nav.tilleggsstonader.sak.vedtak.passAvBarn

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
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
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.beregning.PassAvBarnBeregningService
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.AvslagPassAvBarnDto
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.BeregningsresultatPassAvBarnDto
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.InnvilgelsePassAvBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.OpphørPassAvBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.VedtakPassAvBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.validering.ValiderGyldigÅrsakAvslag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedtak/tilsyn-barn")
@ProtectedWithClaims(issuer = "azuread")
class PassAvBarnVedtakController(
    private val beregningService: PassAvBarnBeregningService,
    private val passAvBarnBeregnYtelseSteg: PassAvBarnBeregnYtelseSteg,
    private val stegService: StegService,
    private val tilgangService: TilgangService,
    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
    private val beregningsplanUtleder: BeregningsplanUtleder,
    private val vedtakDtoMapper: VedtakDtoMapper,
    private val validerGyldigÅrsakAvslag: ValiderGyldigÅrsakAvslag,
) {
    @PostMapping("{behandlingId}/innvilgelse")
    fun innvilge(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelsePassAvBarnRequest,
    ): StegFerdigstiltResponse = lagreVedtak(behandlingId, vedtak).tilStegFerdigstiltResponse()

    @PostMapping("{behandlingId}/avslag")
    fun avslå(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: AvslagPassAvBarnDto,
    ): StegFerdigstiltResponse {
        validerGyldigÅrsakAvslag.validerAvslagErGyldig(behandlingId, vedtak.årsakerAvslag, Stønadstype.BARNETILSYN)
        return lagreVedtak(behandlingId, vedtak).tilStegFerdigstiltResponse()
    }

    @PostMapping("{behandlingId}/opphor")
    fun opphør(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: OpphørPassAvBarnRequest,
    ): StegFerdigstiltResponse = lagreVedtak(behandlingId, vedtak).tilStegFerdigstiltResponse()

    fun lagreVedtak(
        behandlingId: BehandlingId,
        vedtak: VedtakPassAvBarnRequest,
    ): Behandling {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        return stegService.håndterSteg(
            behandlingId = behandlingId,
            behandlingSteg = passAvBarnBeregnYtelseSteg,
            data = vedtak,
        )
    }

    @PostMapping("{behandlingId}/beregn")
    fun beregn(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelsePassAvBarnRequest,
    ): BeregningsresultatPassAvBarnDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val vedtaksperioder = vedtak.vedtaksperioder.tilDomene()
        val beregningsplan = beregningsplanUtleder.utledForInnvilgelse(behandling, vedtaksperioder)

        return beregningService
            .beregn(
                vedtaksperioder = vedtaksperioder,
                behandling = behandling,
                plan = beregningsplan,
                typeVedtak = TypeVedtak.INNVILGELSE,
            ).tilDto(beregningsplan)
    }

    @GetMapping("{behandlingId}")
    fun hentVedtak(
        @PathVariable behandlingId: BehandlingId,
    ): VedtakResponse? {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerLesetilgangTilBehandling(behandlingId)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val vedtak = vedtakService.hentVedtak(behandlingId) ?: return null
        return vedtakDtoMapper.toDto(vedtak, saksbehandling.forrigeIverksatteBehandlingId)
    }
}
