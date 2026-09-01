package no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise

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
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.beregning.ReiseOppstartAvslutningHjemreiseBeregningService
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.dto.BeregningsresultatReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.dto.InnvilgelseReiseOppstartAvslutningHjemreiseTsoRequest
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.dto.InnvilgelseReiseOppstartAvslutningHjemreiseTsrRequest
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.dto.VedtakReiseOppstartAvslutningHjemreiseRequest
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.dto.tilDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedtak/reise-oppstart-avslutning-hjemreise")
@ProtectedWithClaims(issuer = "azuread")
class ReiseOppstartAvslutningHjemreiseVedtakController(
    private val behandlingService: BehandlingService,
    private val tilgangService: TilgangService,
    private val vedtakService: VedtakService,
    private val vedtakDtoMapper: VedtakDtoMapper,
    private val beregningService: ReiseOppstartAvslutningHjemreiseBeregningService,
    private val stegService: StegService,
    private val beregnYtelseSteg: ReiseOppstartAvslutningHjemreiseBeregnYtelseSteg,
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
        @RequestBody vedtak: InnvilgelseReiseOppstartAvslutningHjemreiseTsoRequest,
    ): BeregningsresultatReiseOppstartAvslutningHjemreiseDto = beregnVedtak(behandlingId, vedtak.vedtaksperioder())

    @PostMapping("{behandlingId}/tsr/beregn")
    fun beregn(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseReiseOppstartAvslutningHjemreiseTsrRequest,
    ): BeregningsresultatReiseOppstartAvslutningHjemreiseDto = beregnVedtak(behandlingId, vedtak.vedtaksperioder())

    private fun beregnVedtak(
        behandlingId: BehandlingId,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatReiseOppstartAvslutningHjemreiseDto {
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
        @RequestBody vedtak: InnvilgelseReiseOppstartAvslutningHjemreiseTsoRequest,
    ): StegFerdigstiltResponse = lagreVedtak(behandlingId, vedtak).tilStegFerdigstiltResponse()

    @PostMapping("{behandlingId}/tsr/innvilgelse")
    fun innvilge(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseReiseOppstartAvslutningHjemreiseTsrRequest,
    ): StegFerdigstiltResponse = lagreVedtak(behandlingId, vedtak).tilStegFerdigstiltResponse()

    private fun lagreVedtak(
        behandlingId: BehandlingId,
        vedtak: VedtakReiseOppstartAvslutningHjemreiseRequest,
    ): Behandling {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)

        return stegService.håndterSteg(behandlingId, beregnYtelseSteg, vedtak)
    }
}
