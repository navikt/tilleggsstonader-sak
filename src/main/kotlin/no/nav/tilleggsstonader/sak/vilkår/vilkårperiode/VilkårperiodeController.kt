package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.OppdaterGrunnlagDto
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/vilkarperiode"])
@ProtectedWithClaims(issuer = "azuread")
class VilkårperiodeController(
    private val tilgangService: TilgangService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val vilkårperiodeGrunnlagService: VilkårperiodeGrunnlagService,
) {
    @GetMapping("behandling/{behandlingId}")
    fun hentVilkårperioder(
        @PathVariable behandlingId: BehandlingId,
    ): VilkårperioderResponse {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)

        return vilkårperiodeService.hentVilkårperioderResponse(behandlingId)
    }

    @PostMapping("behandling/{behandlingId}/oppdater-grunnlag")
    fun oppdaterGrunnlag(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody oppdaterGrunnlag: OppdaterGrunnlagDto?,
    ) {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        vilkårperiodeGrunnlagService.oppdaterGrunnlag(behandlingId, oppdaterGrunnlag?.hentFom)
    }

    @PostMapping("/v2", "")
    fun opprettVilkårperiode(
        @RequestBody vilkårperiode: LagreVilkårperiode,
    ): LagreVilkårperiodeResponse {
        tilgangService.settBehandlingsdetaljerForRequest(vilkårperiode.behandlingId)
        tilgangService.validerTilgangTilBehandling(vilkårperiode.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        val periode = vilkårperiodeService.opprettVilkårperiode(vilkårperiode)
        return LagreVilkårperiodeResponse(periode = periode.tilDto())
    }

    @PostMapping("/v2/{id}", "/{id}")
    fun oppdaterVilkårperiode(
        @PathVariable("id") id: UUID,
        @RequestBody vilkårperiode: LagreVilkårperiode,
    ): LagreVilkårperiodeResponse {
        tilgangService.settBehandlingsdetaljerForRequest(vilkårperiode.behandlingId)
        tilgangService.validerTilgangTilBehandling(vilkårperiode.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        val periode = vilkårperiodeService.oppdaterVilkårperiode(id, vilkårperiode)
        return LagreVilkårperiodeResponse(periode = periode.tilDto())
    }

    @DeleteMapping("{id}")
    fun slettPeriode(
        @PathVariable("id") id: UUID,
        @RequestBody slettVikårperiode: SlettVikårperiode,
    ): LagreVilkårperiodeResponse {
        tilgangService.settBehandlingsdetaljerForRequest(slettVikårperiode.behandlingId)
        tilgangService.validerTilgangTilBehandling(slettVikårperiode.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        val periode = vilkårperiodeService.slettVilkårperiode(id, slettVikårperiode)

        return LagreVilkårperiodeResponse(periode = periode?.tilDto())
    }
}
