package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseDtoMapper.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseMapper.mapTilVilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreVilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SlettVilkårResultatDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggRegelstrukturFraVilkårregel.tilRegelstruktur
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.DagligReiseRegel
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
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
    private val vilkårperiodeService: VilkårperiodeService,
) {
    @GetMapping("regler")
    fun regler(): RegelstrukturDto = DagligReiseRegel().tilRegelstruktur()

    @GetMapping("{behandlingId}")
    fun hentVilkår(
        @PathVariable behandlingId: BehandlingId,
    ): List<VilkårDagligReiseDto> {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerLesetilgangTilBehandling(behandlingId)

        return dagligReiseVilkårService.hentVilkårForBehandling(behandlingId).map {
            it.tilDtoMedAktivitetType()
        }
    }

    @PostMapping("{behandlingId}")
    fun opprettVilkår(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody lagreVilkårDto: LagreVilkårDagligReiseDto,
    ): VilkårDagligReiseDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)

        return dagligReiseVilkårService
            .opprettNyttVilkår(
                nyttVilkår = lagreVilkårDto.tilDomain(),
                behandlingId = behandlingId,
            ).tilDtoMedAktivitetType()
    }

    @PutMapping("{behandlingId}/{vilkårId}")
    fun oppdaterVilkår(
        @PathVariable behandlingId: BehandlingId,
        @PathVariable vilkårId: VilkårId,
        @RequestBody lagreVilkårDto: LagreVilkårDagligReiseDto,
    ): VilkårDagligReiseDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)

        return dagligReiseVilkårService
            .oppdaterVilkår(
                nyttVilkår = lagreVilkårDto.tilDomain(),
                vilkårId = vilkårId,
                behandlingId = behandlingId,
            ).tilDtoMedAktivitetType()
    }

    @DeleteMapping("{behandlingId}/{vilkårId}")
    fun slettVilkår(
        @PathVariable behandlingId: BehandlingId,
        @PathVariable vilkårId: VilkårId,
        @RequestBody slettVilkårRequestDto: SlettVilkårRequestDto,
    ): SlettVilkårResultatDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.DELETE)

        val slettetVilkårResultat =
            dagligReiseVilkårService
                .slettVilkår(
                    behandlingId = behandlingId,
                    vilkårId = vilkårId,
                    slettetKommentar = slettVilkårRequestDto.kommentar,
                )

        return SlettVilkårResultatDto(
            slettetPermanent = slettetVilkårResultat.slettetPermanent,
            vilkår = slettetVilkårResultat.vilkår.mapTilVilkårDagligReise().tilDtoMedAktivitetType(),
        )
    }

    private fun VilkårDagligReise.tilDtoMedAktivitetType(): VilkårDagligReiseDto {
        val aktivitetType =
            (fakta as? FaktaPrivatBil)?.let {
                vilkårperiodeService.hentAktivitetType(it.aktivitetId, behandlingId)
            }
        return tilDto(aktivitetType = aktivitetType?.name)
    }
}
