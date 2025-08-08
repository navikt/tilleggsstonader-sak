package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.VedtakDtoMapper
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeService
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import no.nav.tilleggsstonader.sak.vedtak.dto.tilLagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.forslag.ForeslåVedtaksperiodeService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.AvslagLæremidlerDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.BeregningsresultatLæremidlerDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.OpphørLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtakLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.tilDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedtak/laremidler")
@ProtectedWithClaims(issuer = "azuread")
class LæremidlerVedtakController(
    private val beregningService: LæremidlerBeregningService,
    private val tilgangService: TilgangService,
    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
    private val stegService: StegService,
    private val steg: LæremidlerBeregnYtelseSteg,
    private val foreslåVedtaksperiodeService: ForeslåVedtaksperiodeService,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
    private val vedtakDtoMapper: VedtakDtoMapper,
    private val vedtaksperiodeService: VedtaksperiodeService,
) {
    @PostMapping("{behandlingId}/innvilgelse")
    fun innvilge(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseLæremidlerRequest,
    ) {
        lagreVedtak(behandlingId, vedtak)
    }

    @PostMapping("{behandlingId}/avslag")
    fun avslå(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: AvslagLæremidlerDto,
    ) {
        lagreVedtak(behandlingId, vedtak)
    }

    @PostMapping("{behandlingId}/opphor")
    fun opphør(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: OpphørLæremidlerRequest,
    ) {
        lagreVedtak(behandlingId, vedtak)
    }

    fun lagreVedtak(
        behandlingId: BehandlingId,
        vedtak: VedtakLæremidlerRequest,
    ) {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        stegService.håndterSteg(behandlingId, steg, vedtak)
    }

    @PostMapping("{behandlingId}/beregn")
    fun beregn(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtaksperioder: List<VedtaksperiodeDto>,
    ): BeregningsresultatLæremidlerDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(
                behandling.id,
                vedtaksperioder.tilDomene(),
            )
        return beregningService
            .beregn(
                behandling,
                vedtaksperioder.tilDomene(),
                tidligsteEndring,
            ).tilDto(tidligsteEndring = tidligsteEndring)
    }

    @GetMapping("{behandlingId}")
    fun hentVedtak(
        @PathVariable behandlingId: BehandlingId,
    ): VedtakResponse? {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val vedtak = vedtakService.hentVedtak(behandlingId) ?: return null
        return vedtakDtoMapper.toDto(vedtak, behandling.revurderFra, behandling.forrigeIverksatteBehandlingId)
    }

    @GetMapping("/fullstendig-oversikt/{behandlingId}")
    fun hentFullstendigVedtaksoversikt(
        @PathVariable behandlingId: BehandlingId,
    ): VedtakResponse? {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val vedtak = vedtakService.hentVedtak(behandlingId) ?: return null
        return vedtakDtoMapper.toDto(vedtak, null, behandling.forrigeIverksatteBehandlingId)
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
                vedtaksperiodeService.finnVedtaksperioderForBehandling(
                    behandlingId = it,
                    revurdererFra = null,
                )
            }

        return foreslåVedtaksperiodeService
            .foreslåPerioder(behandlingId)
            .tilLagretVedtaksperiodeDto(
                tidligereVedtaksperioder = forrigeVedtaksperioder,
            )
    }
}
