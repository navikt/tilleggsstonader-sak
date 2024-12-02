package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.VedtakDtoMapper
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.AvslagLæremidlerDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtakLæremidlerRequest
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
) {

    /*
    @PostMapping("{behandlingId}/innvilgelse")
    fun innvilge(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseTilsynBarnRequest,
    ) {
        lagreVedtak(behandlingId, vedtak)
    }
     */

    @PostMapping("{behandlingId}/avslag")
    fun avslå(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: AvslagLæremidlerDto,
    ) {
        lagreVedtak(behandlingId, vedtak)
    }

    /*
    @PostMapping("{behandlingId}/opphor")
    fun opphør(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: OpphørTilsynBarnDto,
    ) {
        lagreVedtak(behandlingId, vedtak)
    }
     */

    fun lagreVedtak(behandlingId: BehandlingId, vedtak: VedtakLæremidlerRequest) {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        stegService.håndterSteg(behandlingId, steg, vedtak)
    }

    /*
    @PostMapping("{behandlingId}/beregn")
    fun beregn(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseTilsynBarnRequest,
    ): BeregningsresultatTilsynBarnDto {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        return tilsynBarnBeregningService.beregn(behandling, TypeVedtak.INNVILGELSE).tilDto(behandling.revurderFra)
    }
     */

    /**
     * TODO Post og Get burde kanskje håndtere 2 ulike objekt?
     * På en måte hadde det vært fint hvis GET returnerer beløpsperioder
     */
    @GetMapping("{behandlingId}")
    fun hentVedtak(@PathVariable behandlingId: BehandlingId): VedtakResponse? {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        val revurderFra = behandlingService.hentSaksbehandling(behandlingId).revurderFra
        val vedtak = vedtakService.hentVedtak(behandlingId) ?: return null
        return VedtakDtoMapper.toDto(vedtak, revurderFra)
    }

    @GetMapping("/historisk/{behandlingId}")
    fun hentHistoriskVedtak(@PathVariable behandlingId: BehandlingId): VedtakResponse? {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        val vedtak = vedtakService.hentVedtak(behandlingId) ?: return null
        return VedtakDtoMapper.toDto(vedtak, null)
    }
}
