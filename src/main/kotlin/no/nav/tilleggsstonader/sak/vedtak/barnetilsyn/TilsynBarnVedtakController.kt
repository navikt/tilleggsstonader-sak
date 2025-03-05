package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakDtoMapper
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.BeregningsresultatTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnRequestV2
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedtak/tilsyn-barn")
@ProtectedWithClaims(issuer = "azuread")
class TilsynBarnVedtakController(
    private val beregningService: TilsynBarnBeregningService,
    private val tilgangService: TilgangService,
    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
    private val vedtaksperiodeService: VedtaksperiodeService,
) {
    @PostMapping("{behandlingId}/innvilgelseV2")
    fun innvilgeV2(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseTilsynBarnRequestV2,
    ) {
        lagreVedtak(behandlingId, vedtak)
    }

    @PostMapping("{behandlingId}/avslag")
    fun avslå(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: AvslagTilsynBarnDto,
    ) {
        lagreVedtak(behandlingId, vedtak)
    }

    @PostMapping("{behandlingId}/opphor")
    fun opphør(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: OpphørTilsynBarnRequest,
    ) {
        lagreVedtak(behandlingId, vedtak)
    }

    fun lagreVedtak(
        behandlingId: BehandlingId,
        vedtak: VedtakTilsynBarnRequest,
    ) {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        vedtakService.håndterSteg(behandlingId, vedtak)
    }

    @PostMapping("{behandlingId}/beregnV2")
    fun beregnV2(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseTilsynBarnRequestV2,
    ): BeregningsresultatTilsynBarnDto {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        return beregningService
            .beregnV2(
                vedtaksperioder = vedtak.vedtaksperioder.tilDomene(),
                behandling = behandling,
                typeVedtak = TypeVedtak.INNVILGELSE,
            ).tilDto(behandling.revurderFra)
    }

    /**
     * TODO Post og Get burde kanskje håndtere 2 ulike objekt?
     * På en måte hadde det vært fint hvis GET returnerer beløpsperioder
     */
    @GetMapping("{behandlingId}")
    fun hentVedtak(
        @PathVariable behandlingId: BehandlingId,
    ): VedtakResponse? {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        val revurderFra = behandlingService.hentSaksbehandling(behandlingId).revurderFra
        val vedtak = vedtakService.hentVedtak(behandlingId) ?: return null
        return VedtakDtoMapper.toDto(vedtak, revurderFra)
    }

    @GetMapping("/fullstendig-oversikt/{behandlingId}")
    fun hentFullstendigVedtaksoversikt(
        @PathVariable behandlingId: BehandlingId,
    ): VedtakResponse? {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        val vedtak = vedtakService.hentVedtak(behandlingId) ?: return null
        return VedtakDtoMapper.toDto(vedtak, null)
    }

    @GetMapping("{behandlingId}/foresla")
    fun foreslåVedtaksperioder(
        @PathVariable behandlingId: BehandlingId,
    ): List<VedtaksperiodeDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarSaksbehandlerrolle()

        return vedtaksperiodeService.foreslåPerioder(behandlingId).map { it.tilDto() }
    }
}
