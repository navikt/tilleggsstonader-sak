package no.nav.tilleggsstonader.sak.vedtak.flytting

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.gjelderFlytting
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegFerdigstiltResponse
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.behandlingsflyt.tilStegFerdigstiltResponse
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.BeregningsplanUtleder
import no.nav.tilleggsstonader.sak.vedtak.VedtakDtoMapper
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.flytting.beregning.FlyttingBeregningService
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.AvslagFlyttingDto
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.BeregningsresultatFlyttingDto
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.InnvilgelseFlyttingRequest
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.InnvilgelseFlyttingTsoRequest
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.InnvilgelseFlyttingTsrRequest
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.VedtakFlyttingRequest
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.validering.ValiderGyldigÅrsakAvslag
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
    private val validerGyldigÅrsakAvslag: ValiderGyldigÅrsakAvslag,
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
    ): StegFerdigstiltResponse = lagreVedtak(behandlingId, vedtak, Stønadstype.FLYTTING_TSO).tilStegFerdigstiltResponse()

    @PostMapping("{behandlingId}/tsr/innvilgelse")
    fun innvilgeTsr(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: InnvilgelseFlyttingTsrRequest,
    ): StegFerdigstiltResponse = lagreVedtak(behandlingId, vedtak, Stønadstype.FLYTTING_TSR).tilStegFerdigstiltResponse()

    @PostMapping("{behandlingId}/avslag")
    fun avslå(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody vedtak: AvslagFlyttingDto,
    ): StegFerdigstiltResponse = lagreVedtak(behandlingId, vedtak).tilStegFerdigstiltResponse()

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
        tilgangService.validerLesetilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerStønadstype(behandling, forventetStønadstype)
        val vedtaksperioder = vedtak.vedtaksperioder()
        val beregningsplan = beregningsplanUtleder.utledForInnvilgelse(behandling, vedtaksperioder)
        return beregningService.beregn(behandling, vedtaksperioder, beregningsplan).tilDto()
    }

    private fun lagreVedtak(
        behandlingId: BehandlingId,
        vedtak: VedtakFlyttingRequest,
        forventetStønadstype: Stønadstype? = null,
    ): Behandling {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        forventetStønadstype?.let { validerStønadstype(behandling, it) }
        if (vedtak is AvslagFlyttingDto) {
            brukerfeilHvisIkke(behandling.stønadstype.gjelderFlytting()) {
                "Forventet stønadstype for flytteendepunkt"
            }
            validerGyldigÅrsakAvslag.validerAvslagErGyldig(
                behandlingId = behandlingId,
                årsakerAvslag = vedtak.årsakerAvslag,
                stønadstype = behandling.stønadstype,
            )
        }
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
}
