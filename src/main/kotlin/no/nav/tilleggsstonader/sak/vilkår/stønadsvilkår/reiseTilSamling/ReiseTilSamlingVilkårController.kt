package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggRegelstrukturFraVilkårregel.tilRegelstruktur
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.ReiseTilSamlingRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.VilkårReiseTilSamlingDtoMapper.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.VilkårReiseTilSamlingMapper.mapTilVilkårReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.VilkårReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.LagreVilkårReiseTilSamlingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.SlettVilkårResultatDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.VilkårReiseTilSamlingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.AktivitetPåFaktaDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilAktivitetPåFaktaDto
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
@RequestMapping(path = ["/api/vilkar/reise-til-samling"])
@ProtectedWithClaims(issuer = "azuread")
class ReiseTilSamlingVilkårController(
    private val tilgangService: TilgangService,
    private val reiseTilSamlingVilkårService: ReiseTilSamlingVilkårService,
    private val vilkårperiodeService: VilkårperiodeService,
) {
    @GetMapping("regler")
    fun regler(): RegelstrukturDto = ReiseTilSamlingRegel().tilRegelstruktur()

    @GetMapping("{behandlingId}")
    fun hentVilkår(
        @PathVariable behandlingId: BehandlingId,
    ): List<VilkårReiseTilSamlingDto> {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerLesetilgangTilBehandling(behandlingId)

        return reiseTilSamlingVilkårService.hentVilkårForBehandling(behandlingId).map {
            it.tilDtoMedAktivitet(behandlingId)
        }
    }

    @PostMapping("{behandlingId}")
    fun opprettVilkår(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody lagreVilkårDto: LagreVilkårReiseTilSamlingDto,
    ): VilkårReiseTilSamlingDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)

        return reiseTilSamlingVilkårService
            .opprettNyttVilkår(
                nyttVilkår = lagreVilkårDto.tilDomain(),
                behandlingId = behandlingId,
            ).tilDtoMedAktivitet(behandlingId)
    }

    @PutMapping("{behandlingId}/{vilkårId}")
    fun oppdaterVilkår(
        @PathVariable behandlingId: BehandlingId,
        @PathVariable vilkårId: VilkårId,
        @RequestBody lagreVilkårDto: LagreVilkårReiseTilSamlingDto,
    ): VilkårReiseTilSamlingDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)

        return reiseTilSamlingVilkårService
            .oppdaterVilkår(
                nyttVilkår = lagreVilkårDto.tilDomain(),
                vilkårId = vilkårId,
                behandlingId = behandlingId,
            ).tilDtoMedAktivitet(behandlingId)
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
            reiseTilSamlingVilkårService
            .slettVilkår(
                behandlingId = behandlingId,
                vilkårId = vilkårId,
                slettetKommentar = slettVilkårRequestDto.kommentar,
            )

        return SlettVilkårResultatDto(
            slettetPermanent = slettetVilkårResultat.slettetPermanent,
            vilkår = slettetVilkårResultat.vilkår.mapTilVilkårReiseTilSamling().tilDtoMedAktivitet(behandlingId),
        )
    }

    private fun VilkårReiseTilSamling.tilDtoMedAktivitet(behandlingId: BehandlingId): VilkårReiseTilSamlingDto {
        val aktivitet = fakta.aktivitetPåFakta(behandlingId)
        return tilDto(aktivitet = aktivitet)
    }

    private fun FaktaReiseTilSamling.aktivitetPåFakta(behandlingId: BehandlingId): AktivitetPåFaktaDto? =
        when (this) {
            is FaktaOffentligTransport ->
                this.aktivitetId?.let { vilkårperiodeService.hentAktivitet(it, behandlingId) }?.tilAktivitetPåFaktaDto()
            is FaktaPrivatBil ->
                this.aktivitetId?.let { vilkårperiodeService.hentAktivitet(it, behandlingId) }?.tilAktivitetPåFaktaDto()
                    ?: error("Finner ikke aktivitet for privat bil med aktivitetId=${this.aktivitetId}")
            is FaktaUbestemtType -> null
        }
}
