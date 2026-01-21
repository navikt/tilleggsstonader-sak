package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakDtoMapper
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.DagligReiseBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.AvslagDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningsresultatDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.OpphørDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.VedtakDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
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
    private val vedtakSteg: DagligReiseVedtakSteg,
    private val vedtakService: VedtakService,
    private val vedtakDtoMapper: VedtakDtoMapper,
    private val validerGyldigÅrsakAvslag: ValiderGyldigÅrsakAvslag,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
    private val unleashService: UnleashService,
    private val dagligReiseVilkårService: DagligReiseVilkårService,
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

    @PostMapping("{behandlingId}/beregn")
    fun beregn(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseDagligReiseRequest,
    ): BeregningsresultatDagligReiseDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(
                behandling.id,
                vedtak.vedtaksperioder.tilDomene(),
            )
        val beregningsresultat =
            beregningService
                .beregn(
                    vedtaksperioder = vedtak.vedtaksperioder.tilDomene(),
                    behandling = behandling,
                    typeVedtak = TypeVedtak.INNVILGELSE,
                    tidligsteEndring = tidligsteEndring,
                )

        val vilkår = dagligReiseVilkårService.hentVilkårForBehandling(behandlingId)
        return beregningsresultat.tilDto(tidligsteEndring = tidligsteEndring, vilkår)
    }

    private fun lagreVedtak(
        behandlingId: BehandlingId,
        vedtak: VedtakDagligReiseRequest,
    ) {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)

        val steg = finnRiktigSteg()
        stegService.håndterSteg(behandlingId, steg, vedtak)
    }

    private fun finnRiktigSteg(): BehandlingSteg<VedtakDagligReiseRequest> {
        if (unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL)) {
            return vedtakSteg
        }
        return beregnYtelseSteg
    }
}
