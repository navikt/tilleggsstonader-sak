package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.VilkårFlyttingMapper.tilVilkårFlyttingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.LagreVilkårFlyttingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.SlettVilkårFlyttingRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.SlettVilkårFlyttingResultatDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.VilkårFlyttingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.tilVilkårFlyttingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggRegelstrukturFraVilkårregel.tilRegelstruktur
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.FlyttingRegel
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/vilkar/flytting"])
@ProtectedWithClaims(issuer = "azuread")
class FlyttingVilkårController(
    private val tilgangService: TilgangService,
    private val flyttingVilkårService: FlyttingVilkårService,
) {
    @GetMapping("regler")
    fun regler(): RegelstrukturDto = FlyttingRegel().tilRegelstruktur()

    @GetMapping("{behandlingId}")
    fun hentVilkår(
        @PathVariable behandlingId: BehandlingId,
    ): List<VilkårFlyttingDto> {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerLesetilgangTilBehandling(behandlingId)
        return flyttingVilkårService.hentVilkårForBehandling(behandlingId).map { it.tilVilkårFlyttingDto() }
    }

    @PostMapping("{behandlingId}")
    fun opprettVilkår(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody lagreVilkår: LagreVilkårFlyttingDto,
    ): VilkårFlyttingDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        return flyttingVilkårService.opprettVilkår(behandlingId, lagreVilkår.tilDomain()).tilVilkårFlyttingDto()
    }

    @PutMapping("{behandlingId}/{vilkårId}")
    fun oppdaterVilkår(
        @PathVariable behandlingId: BehandlingId,
        @PathVariable vilkårId: VilkårId,
        @RequestBody lagreVilkår: LagreVilkårFlyttingDto,
    ): VilkårFlyttingDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        return flyttingVilkårService
            .oppdaterVilkår(behandlingId, vilkårId, lagreVilkår.tilDomain())
            .tilVilkårFlyttingDto()
    }

    @DeleteMapping("{behandlingId}/{vilkårId}")
    fun slettVilkår(
        @PathVariable behandlingId: BehandlingId,
        @PathVariable vilkårId: VilkårId,
        @RequestBody request: SlettVilkårFlyttingRequestDto,
    ): SlettVilkårFlyttingResultatDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.DELETE)
        return flyttingVilkårService.slettVilkår(behandlingId, vilkårId, request.kommentar).tilVilkårFlyttingDto()
    }
}
