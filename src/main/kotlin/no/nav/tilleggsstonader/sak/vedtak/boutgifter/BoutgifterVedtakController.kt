package no.nav.tilleggsstonader.sak.vedtak.boutgifter

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregningService
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.BeregningsresultatBoutgifterDto
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedtak/boutgifter")
@ProtectedWithClaims(issuer = "azuread")
class BoutgifterVedtakController(
    private val beregningService: BoutgifterBeregningService,
//    private val tilgangService: TilgangService,
//    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
//    private val stegService: StegService,
//    private val steg: BoutgifterBeregnYtelseSteg,
) {
//    @PostMapping("{behandlingId}/innvilgelse")
//    fun innvilge(
//        @PathVariable behandlingId: BehandlingId,
//        @RequestBody vedtak: InnvilgelseBoutgifterRequest,
//    ) {
//        lagreVedtak(behandlingId, vedtak)
//    }

//    @PostMapping("{behandlingId}/avslag")
//    fun avslå(
//        @PathVariable behandlingId: BehandlingId,
//        @RequestBody vedtak: AvslagBoutgifterDto,
//    ) {
//        lagreVedtak(behandlingId, vedtak)
//    }

//    @PostMapping("{behandlingId}/opphor")
//    fun opphør(
//        @PathVariable behandlingId: BehandlingId,
//        @RequestBody vedtak: OpphørBoutgifterRequest,
//    ) {
//        lagreVedtak(behandlingId, vedtak)
//    }

//    fun lagreVedtak(
//        behandlingId: BehandlingId,
//        vedtak: VedtakBoutgifterRequest,
//    ) {
//        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
//        stegService.håndterSteg(behandlingId, steg, vedtak)
//    }

    @PostMapping("{behandlingId}/beregn")
    fun beregn(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtaksperioder: List<VedtaksperiodeDto>,
    ): BeregningsresultatBoutgifterDto {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        return beregningService
            .beregn(behandling, vedtaksperioder.tilDomene())
            .tilDto(revurderFra = behandling.revurderFra)
    }

//    @GetMapping("{behandlingId}")
//    fun hentVedtak(
//        @PathVariable behandlingId: BehandlingId,
//    ): VedtakResponse? {
//        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
//        val revurderFra = behandlingService.hentSaksbehandling(behandlingId).revurderFra
//        val vedtak = vedtakService.hentVedtak(behandlingId) ?: return null
//        return VedtakDtoMapper.toDto(vedtak, revurderFra)
//    }

//    @GetMapping("/fullstendig-oversikt/{behandlingId}")
//    fun hentFullstendigVedtaksoversikt(
//        @PathVariable behandlingId: BehandlingId,
//    ): VedtakResponse? {
//        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
//        val vedtak = vedtakService.hentVedtak(behandlingId) ?: return null
//        return VedtakDtoMapper.toDto(vedtak, null)
//    }
}
