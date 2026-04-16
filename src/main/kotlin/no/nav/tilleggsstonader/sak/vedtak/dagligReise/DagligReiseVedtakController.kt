package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.BeregningsplanUtleder
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakDtoMapper
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.DagligReiseBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.AvslagDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseTsoRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseTsrRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.OpphørDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.VedtakDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.validering.ValiderGyldigÅrsakAvslag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.DagligReiseVilkårService
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
    private val beregningService: DagligReiseBeregningService,
    private val tilgangService: TilgangService,
    private val stegService: StegService,
    private val beregnYtelseSteg: DagligReiseBeregnYtelseSteg,
    private val vedtakService: VedtakService,
    private val vedtakDtoMapper: VedtakDtoMapper,
    private val validerGyldigÅrsakAvslag: ValiderGyldigÅrsakAvslag,
    private val beregningsplanUtleder: BeregningsplanUtleder,
    private val dagligReiseVilkårService: DagligReiseVilkårService,
) {
    @PostMapping("{behandlingId}/tso/innvilgelse")
    fun innvilge(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseDagligReiseTsoRequest,
    ) {
        lagreVedtak(behandlingId, vedtak)
    }

    @PostMapping("{behandlingId}/tsr/innvilgelse")
    fun innvilge(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseDagligReiseTsrRequest,
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
        tilgangService.validerLesetilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val vedtak = vedtakService.hentVedtak(behandlingId) ?: return null
        return vedtakDtoMapper.toDto(vedtak, behandling.forrigeIverksatteBehandlingId)
    }

    @PostMapping("{behandlingId}/opphor")
    fun opphor(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: OpphørDagligReiseRequest,
    ) {
        lagreVedtak(behandlingId, vedtak)
    }

    @PostMapping("{behandlingId}/tso/beregn")
    fun beregn(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseDagligReiseTsoRequest,
    ): BeregningDagligReiseDto = beregnVedtak(behandlingId, vedtak.vedtaksperioder())

    @PostMapping("{behandlingId}/tsr/beregn")
    fun beregn(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseDagligReiseTsrRequest,
    ): BeregningDagligReiseDto = beregnVedtak(behandlingId, vedtak.vedtaksperioder())

    @GetMapping("{behandlingId}/privat-bil/oppsummer-beregning")
    fun hentOppsummertBeregning(
        @PathVariable behandlingId: BehandlingId,
    ): PrivatBilOppsummertBeregningDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerLesetilgangTilBehandling(behandlingId)

        val vedtaksdata = vedtakService.hentVedtak<InnvilgelseEllerOpphørDagligReise>(behandlingId).data
        val beregningsresultatPrivatBil = vedtaksdata.beregningsresultat.privatBil
        val rammevedtak = vedtaksdata.rammevedtakPrivatBil

        feilHvis(beregningsresultatPrivatBil == null || rammevedtak == null) {
            "Det finnes ingen beregningsresultat for privat bil eller rammevedtak for privat bil for behandling $behandlingId"
        }

        return oppsummerBeregningPrivatBil(beregningsresultatPrivatBil, rammevedtak)
    }

    private fun beregnVedtak(
        behandlingId: BehandlingId,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningDagligReiseDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val plan = beregningsplanUtleder.utledForInnvilgelse(behandling, vedtaksperioder)
        val beregningsresultat =
            beregningService.beregn(
                vedtaksperioder = vedtaksperioder,
                behandling = behandling,
                beregningsplan = plan,
                typeVedtak = TypeVedtak.INNVILGELSE,
            )

        val vilkår = dagligReiseVilkårService.hentVilkårForBehandling(behandlingId)
        return beregningsresultat.tilDto(beregningsplan = plan, vilkår)
    }

    private fun lagreVedtak(
        behandlingId: BehandlingId,
        vedtak: VedtakDagligReiseRequest,
    ) {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)

        stegService.håndterSteg(behandlingId, beregnYtelseSteg, vedtak)
    }
}
