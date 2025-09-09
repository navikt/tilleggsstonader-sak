package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.VedtakDtoMapper
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.OffentligTransportBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.AvslagDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.VedtakDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import no.nav.tilleggsstonader.sak.vedtak.dto.tilLagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.forslag.ForeslåVedtaksperiodeService
import no.nav.tilleggsstonader.sak.vedtak.validering.ValiderGyldigÅrsakAvslag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedtak/daglig-reise")
@ProtectedWithClaims(issuer = "azuread")
class DagligReiseVedtakController(
    private val behandlingService: BehandlingService,
    private val beregningService: OffentligTransportBeregningService,
    private val tilgangService: TilgangService,
    private val stegService: StegService,
    private val steg: DagligReiseBeregnYtelseSteg,
    private val vedtakService: VedtakService,
    private val vedtakDtoMapper: VedtakDtoMapper,
    private val foreslåVedtaksperiodeService: ForeslåVedtaksperiodeService,
    private val validerGyldigÅrsakAvslag: ValiderGyldigÅrsakAvslag,
    private val fagsakService: FagsakService,
) {
    @PostMapping("{behandlingId}/innvilgelse")
    fun innvilge(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseDagligReiseRequest,
    ) {
        lagreVedtak(behandlingId, vedtak)
    }

    @PostMapping("{behandlingId}/avslag")
    fun avslå(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: AvslagDagligReiseDto,
    ) {
        val stønadsType = behandlingService.hentSaksbehandling(behandlingId).stønadstype

        validerGyldigÅrsakAvslag.validerAvslagErGyldig(
            behandlingId,
            vedtak.årsakerAvslag,
            stønadsType,
        )

        lagreVedtak(behandlingId, vedtak)
    }

    @GetMapping("{behandlingId}")
    fun hentVedtak(
        @PathVariable behandlingId: BehandlingId,
    ): VedtakResponse? {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val vedtak = vedtakService.hentVedtak(behandlingId) ?: return null
        return vedtakDtoMapper.toDto(vedtak, behandling.forrigeIverksatteBehandlingId)
    }

    @PostMapping("{behandlingId}/beregn")
    fun beregn(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseDagligReiseRequest,
    ): Beregningsresultat {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        return beregningService
            .beregn(
                behandlingId = behandlingId,
                vedtaksperioder = vedtak.vedtaksperioder.tilDomene(),
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

    private fun lagreVedtak(
        behandlingId: BehandlingId,
        vedtak: VedtakDagligReiseRequest,
    ) {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        stegService.håndterSteg(behandlingId, steg, vedtak)
    }
}
