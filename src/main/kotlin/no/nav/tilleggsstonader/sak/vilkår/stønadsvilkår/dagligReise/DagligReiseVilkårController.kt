package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseDtoMapper.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SlettVilkårResultatDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.tilDagligreiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggRegelstrukturFraVilkårregel.tilRegelstruktur
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.DagligReiseOffentiligTransportRegel
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/vilkar/daglig-reise"])
@ProtectedWithClaims(issuer = "azuread")
class DagligReiseVilkårController(
    private val tilgangService: TilgangService,
    private val dagligReiseVilkårService: DagligReiseVilkårService,
) {
    @GetMapping("regler")
    fun regler(): RegelstrukturDto = DagligReiseOffentiligTransportRegel().tilRegelstruktur()

    @GetMapping("{behandlingId}")
    fun hentVilkår(
        @PathVariable behandlingId: BehandlingId,
    ): List<VilkårDagligReiseDto> {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)

        return dagligReiseVilkårService.hentVilkårForBehandling(behandlingId).map { it.tilDto() }
    }

    @PostMapping("{behandlingId}")
    fun opprettVilkår(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody lagreVilkårDto: LagreDagligReiseDto,
    ): VilkårDagligReiseDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return dagligReiseVilkårService
            .opprettNyttVilkår(
                nyttVilkår = lagreVilkårDto.tilDomain(),
                behandlingId = behandlingId,
            ).tilDto()
    }

    @PutMapping("{behandlingId}/{vilkårId}")
    fun oppdaterVilkår(
        @PathVariable behandlingId: BehandlingId,
        @PathVariable vilkårId: VilkårId,
        @RequestBody lagreVilkårDto: LagreDagligReiseDto,
    ): VilkårDagligReiseDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return dagligReiseVilkårService
            .oppdaterVilkår(
                nyttVilkår = lagreVilkårDto.tilDomain(),
                vilkårId = vilkårId,
                behandlingId = behandlingId,
            ).tilDto()
    }

    @DeleteMapping("{behandlingId}/{vilkårId}")
    fun slettVilkår(
        @PathVariable behandlingId: BehandlingId,
        @PathVariable vilkårId: VilkårId,
        @RequestBody slettVilkårRequestDto: SlettVilkårRequestDto,
    ): SlettVilkårResultatDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.DELETE)
        tilgangService.validerHarSaksbehandlerrolle()

        return dagligReiseVilkårService
            .slettVilkår(
                behandlingId = behandlingId,
                vilkårId = vilkårId,
                slettetKommentar = slettVilkårRequestDto.kommentar,
            ).tilDagligreiseDto()
    }
}
