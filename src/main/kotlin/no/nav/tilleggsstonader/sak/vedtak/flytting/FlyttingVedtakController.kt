package no.nav.tilleggsstonader.sak.vedtak.flytting

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegFerdigstiltResponse
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.behandlingsflyt.tilStegFerdigstiltResponse
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.BeregningsplanUtleder
import no.nav.tilleggsstonader.sak.vedtak.VedtakDtoMapper
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.flytting.beregning.FlyttingBeregningService
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.BeregningsresultatFlyttingDto
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.InnvilgelseFlyttingRequest
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.InnvilgelseFlyttingTsoRequest
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.InnvilgelseFlyttingTsrRequest
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.tilDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedtak/flytting")
@ProtectedWithClaims(issuer = "azuread")
class FlyttingVedtakController(
    private val behandlingService: BehandlingService,
    private val tilgangService: TilgangService,
    private val vedtakService: VedtakService,
    private val vedtakDtoMapper: VedtakDtoMapper,
    private val beregningService: FlyttingBeregningService,
    private val stegService: StegService,
    private val beregnYtelseSteg: FlyttingBeregnYtelseSteg,
    private val beregningsplanUtleder: BeregningsplanUtleder,
) {
    @PostMapping("{behandlingId}/tso/beregn")
    fun beregnTso(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseFlyttingTsoRequest,
    ): BeregningsresultatFlyttingDto = beregn(behandlingId, Stønadstype.FLYTTING_TSO, vedtak)

    @PostMapping("{behandlingId}/tsr/beregn")
    fun beregnTsr(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseFlyttingTsrRequest,
    ): BeregningsresultatFlyttingDto = beregn(behandlingId, Stønadstype.FLYTTING_TSR, vedtak)

    @PostMapping("{behandlingId}/tso/innvilgelse")
    fun innvilgeTso(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseFlyttingTsoRequest,
    ): StegFerdigstiltResponse = lagreVedtak(behandlingId, Stønadstype.FLYTTING_TSO, vedtak).tilStegFerdigstiltResponse()

    @PostMapping("{behandlingId}/tsr/innvilgelse")
    fun innvilgeTsr(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseFlyttingTsrRequest,
    ): StegFerdigstiltResponse = lagreVedtak(behandlingId, Stønadstype.FLYTTING_TSR, vedtak).tilStegFerdigstiltResponse()

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

    private fun beregn(
        behandlingId: BehandlingId,
        forventetStønadstype: Stønadstype,
        vedtak: InnvilgelseFlyttingRequest,
    ): BeregningsresultatFlyttingDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerStønadstype(behandling, forventetStønadstype)
        validerPoCOmfång(behandling)
        val vedtaksperioder = vedtak.vedtaksperioder()
        beregningsplanUtleder.utledForInnvilgelse(behandling, vedtaksperioder)
        return beregningService.beregn(behandling, vedtaksperioder).tilDto()
    }

    private fun lagreVedtak(
        behandlingId: BehandlingId,
        forventetStønadstype: Stønadstype,
        vedtak: InnvilgelseFlyttingRequest,
    ): Behandling {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerStønadstype(behandling, forventetStønadstype)
        return stegService.håndterSteg(behandlingId, beregnYtelseSteg, vedtak)
    }

    private fun validerStønadstype(
        behandling: Saksbehandling,
        forventetStønadstype: Stønadstype,
    ) {
        brukerfeilHvisIkke(behandling.stønadstype == forventetStønadstype) {
            "Forventet stønadstype=$forventetStønadstype for flytteendepunkt"
        }
    }

    private fun validerPoCOmfång(behandling: Saksbehandling) {
        brukerfeilHvisIkke(behandling.steg == StegType.BEREGNE_YTELSE) {
            "Flyttevedtak kan bare beregnes på steget BEREGNE_YTELSE"
        }
        brukerfeilHvisIkke(behandling.forrigeIverksatteBehandlingId == null) {
            "Revurdering av flyttevedtak støttes ikke i PoC-en"
        }
    }
}
