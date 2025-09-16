package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakDtoMapper
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.detaljerteVedtaksperioder.DetaljertVedtaksperiodeTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.BeregningsresultatTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import no.nav.tilleggsstonader.sak.vedtak.validering.ValiderGyldigÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt.VedtaksperioderOversiktService
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
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
    private val vedtakDtoMapper: VedtakDtoMapper,
    private val validerGyldigÅrsakAvslag: ValiderGyldigÅrsakAvslag,
    private val vedtakOversiktService: VedtaksperioderOversiktService,
) {
    @PostMapping("{behandlingId}/innvilgelse")
    fun innvilge(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseTilsynBarnRequest,
    ) {
        lagreVedtak(behandlingId, vedtak)
    }

    @PostMapping("{behandlingId}/avslag")
    fun avslå(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: AvslagTilsynBarnDto,
    ) {
        validerGyldigÅrsakAvslag.validerAvslagErGyldig(behandlingId, vedtak.årsakerAvslag, Stønadstype.BARNETILSYN)
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
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        vedtakService.håndterSteg(behandlingId, vedtak)
    }

    @PostMapping("{behandlingId}/beregn")
    fun beregn(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseTilsynBarnRequest,
    ): BeregningsresultatTilsynBarnDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val vedtaksperioder = vedtak.vedtaksperioder.tilDomene()
        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(
                behandling.id,
                vedtaksperioder,
            )

        return beregningService
            .beregn(
                vedtaksperioder = vedtaksperioder,
                behandling = behandling,
                typeVedtak = TypeVedtak.INNVILGELSE,
                tidligsteEndring = tidligsteEndring,
            ).tilDto(tidligsteEndring)
    }

    @GetMapping("{behandlingId}")
    fun hentVedtak(
        @PathVariable behandlingId: BehandlingId,
    ): VedtakResponse? {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val vedtak = vedtakService.hentVedtak(behandlingId) ?: return null
        return vedtakDtoMapper.toDto(vedtak, saksbehandling.forrigeIverksatteBehandlingId)
    }

    @GetMapping("/fullstendig-oversikt/{behandlingId}")
    fun hentFullstendigVedtaksoversikt(
        @PathVariable behandlingId: BehandlingId,
    ): VedtakResponse? {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        val vedtak = vedtakService.hentVedtak(behandlingId) ?: return null
        return vedtakDtoMapper.toDto(vedtak, behandlingService.hentBehandling(behandlingId).forrigeIverksatteBehandlingId)
    }

    @GetMapping("/oversikt/{fagsakId}")
    fun hentDetaljertVedtaksperioder(
        @PathVariable fagsakId: FagsakId,
    ): List<DetaljertVedtaksperiodeTilsynBarn> = vedtakOversiktService.oppsummerVedtaksperioderTilsynBarn(fagsakId)
}
