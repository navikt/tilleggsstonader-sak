package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeNy
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderResponse
import org.springframework.validation.annotation.Validated
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
@Validated
class VilkårperiodeController(
    private val tilgangService: TilgangService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val vilkårperiodeGrunnlagService: VilkårperiodeGrunnlagService,
) {

    @GetMapping("behandling/{behandlingId}")
    fun hentVilkårperioder(@PathVariable behandlingId: BehandlingId): VilkårperioderResponse {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)

        return vilkårperiodeService.hentVilkårperioderResponse(behandlingId)
    }

    @PostMapping("behandling/{behandlingId}/oppdater-grunnlag")
    fun oppdaterGrunnlag(@PathVariable behandlingId: BehandlingId) {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        vilkårperiodeGrunnlagService.oppdaterGrunnlag(behandlingId)
    }

    @PostMapping
    fun opprettVilkårMedPeriode(
        @RequestBody vilkårperiode: LagreVilkårperiode,
    ): LagreVilkårperiodeResponse {
        tilgangService.validerTilgangTilBehandling(vilkårperiode.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        val periode = vilkårperiodeService.opprettVilkårperiode(vilkårperiode)
        return vilkårperiodeService.validerOgLagResponse(behandlingId = periode.behandlingId, periode = periode)
    }

    @PostMapping("/v2")
    fun opprettVilkårMedPeriodeV2(
        @RequestBody vilkårperiode: LagreVilkårperiodeNy,
    ): LagreVilkårperiodeResponse {
        tilgangService.validerTilgangTilBehandling(vilkårperiode.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        val periode = vilkårperiodeService.opprettVilkårperiodeNy(vilkårperiode)
        return vilkårperiodeService.validerOgLagResponse(behandlingId = periode.behandlingId, periode = periode)
    }

    @PostMapping("{id}")
    fun oppdaterPeriode(
        @PathVariable("id") id: UUID,
        @RequestBody vilkårperiode: LagreVilkårperiode,
    ): LagreVilkårperiodeResponse {
        tilgangService.validerTilgangTilBehandling(vilkårperiode.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        val periode = vilkårperiodeService.oppdaterVilkårperiode(id, vilkårperiode)
        return vilkårperiodeService.validerOgLagResponse(behandlingId = periode.behandlingId, periode = periode)
    }

    @PostMapping("/v2/{id}")
    fun oppdaterPeriodeV2(
        @PathVariable("id") id: UUID,
        @RequestBody vilkårperiode: LagreVilkårperiodeNy,
    ): LagreVilkårperiodeResponse {
        tilgangService.validerTilgangTilBehandling(vilkårperiode.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        val periode = vilkårperiodeService.oppdaterVilkårperiodeNy(id, vilkårperiode)
        return vilkårperiodeService.validerOgLagResponse(behandlingId = periode.behandlingId, periode = periode)
    }

    @DeleteMapping("{id}")
    fun slettPeriode(
        @PathVariable("id") id: UUID,
        @RequestBody slettVikårperiode: SlettVikårperiode,
    ): LagreVilkårperiodeResponse {
        tilgangService.validerTilgangTilBehandling(slettVikårperiode.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        val periode = vilkårperiodeService.slettVilkårperiode(id, slettVikårperiode)

        return vilkårperiodeService.validerOgLagResponse(
            behandlingId = slettVikårperiode.behandlingId,
            periode = periode,
        )
    }
}
