package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseDtoMapper.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseMapper.mapTilVilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreVilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SlettVilkårResultatDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.felles.AktivitetInfoDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.felles.tilAktivitetInfoDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggRegelstrukturFraVilkårregel.tilRegelstruktur
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.DagligReiseRegel
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
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
    private val behandlingService: BehandlingService,
) {
    @GetMapping("regler")
    fun regler(): RegelstrukturDto = DagligReiseRegel().tilRegelstruktur()

    @GetMapping("{behandlingId}")
    fun hentVilkår(
        @PathVariable behandlingId: BehandlingId,
    ): List<VilkårDagligReiseDto> {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerLesetilgangTilBehandling(behandlingId)
        val stønadstype = behandlingService.hentSaksbehandling(behandlingId).stønadstype

        return dagligReiseVilkårService.hentVilkårForBehandling(behandlingId).map {
            it.tilDtoMedAktivitet(behandlingId, stønadstype)
        }
    }

    @PostMapping("{behandlingId}")
    fun opprettVilkår(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody lagreVilkårDto: LagreVilkårDagligReiseDto,
    ): VilkårDagligReiseDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        val stønadstype = behandlingService.hentSaksbehandling(behandlingId).stønadstype

        return dagligReiseVilkårService
            .opprettNyttVilkår(
                nyttVilkår = lagreVilkårDto.tilDomain(),
                behandlingId = behandlingId,
            ).tilDtoMedAktivitet(behandlingId, stønadstype)
    }

    @PutMapping("{behandlingId}/{vilkårId}")
    fun oppdaterVilkår(
        @PathVariable behandlingId: BehandlingId,
        @PathVariable vilkårId: VilkårId,
        @RequestBody lagreVilkårDto: LagreVilkårDagligReiseDto,
    ): VilkårDagligReiseDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        val stønadstype = behandlingService.hentSaksbehandling(behandlingId).stønadstype

        return dagligReiseVilkårService
            .oppdaterVilkår(
                nyttVilkår = lagreVilkårDto.tilDomain(),
                vilkårId = vilkårId,
                behandlingId = behandlingId,
            ).tilDtoMedAktivitet(behandlingId, stønadstype)
    }

    @DeleteMapping("{behandlingId}/{vilkårId}")
    fun slettVilkår(
        @PathVariable behandlingId: BehandlingId,
        @PathVariable vilkårId: VilkårId,
        @RequestBody slettVilkårRequestDto: SlettVilkårRequestDto,
    ): SlettVilkårResultatDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.DELETE)
        val stønadstype = behandlingService.hentSaksbehandling(behandlingId).stønadstype

        val slettetVilkårResultat =
            dagligReiseVilkårService
                .slettVilkår(
                    behandlingId = behandlingId,
                    vilkårId = vilkårId,
                    slettetKommentar = slettVilkårRequestDto.kommentar,
                )

        return SlettVilkårResultatDto(
            slettetPermanent = slettetVilkårResultat.slettetPermanent,
            vilkår = slettetVilkårResultat.vilkår.mapTilVilkårDagligReise().tilDtoMedAktivitet(behandlingId, stønadstype),
        )
    }

    private fun VilkårDagligReise.tilDtoMedAktivitet(
        behandlingId: BehandlingId,
        stønadstype: Stønadstype,
    ): VilkårDagligReiseDto =
        tilDto(aktivitet = aktivitetInfo(behandlingId, stønadstype))

    private fun VilkårDagligReise.aktivitetInfo(
        behandlingId: BehandlingId,
        stønadstype: Stønadstype,
    ): AktivitetInfoDto? =
        when (val fakta = fakta) {
            is FaktaOffentligTransport -> {
                val obligatoriskAktivitet = stønadstype == Stønadstype.DAGLIG_REISE_TSR
                fakta.aktivitetId?.hentAktivitetInfo(behandlingId, obligatoriskAktivitet)
                    ?: hentAktivitetFraTiltaksvariant(
                        behandlingId = behandlingId,
                        obligatoriskAktivitet = obligatoriskAktivitet,
                        fakta = fakta,
                    )
            }

            is FaktaPrivatBil -> fakta.aktivitetId.hentAktivitetInfo(behandlingId, obligatoriskAktivitet = true)
            is FaktaUbestemtType -> null
        }

    private fun VilkårDagligReise.hentAktivitetFraTiltaksvariant(
        behandlingId: BehandlingId,
        obligatoriskAktivitet: Boolean,
        fakta: FaktaOffentligTransport,
    ): AktivitetInfoDto? {
        val tiltaksvariant = fakta.tiltaksvariant
        if (tiltaksvariant == null) {
            feilHvis(obligatoriskAktivitet) {
                "Mangler aktivitet eller tiltaksvariant på offentlig transport for vilkår=${id.id}"
            }
            return null
        }

        val aktiviteterMedVariant =
            vilkårperiodeService
                .hentVilkårperioder(behandlingId)
                .aktiviteter
                .filter {
                    it.tiltaksvariant == tiltaksvariant &&
                        it.fom <= tom &&
                        it.tom >= fom
                }

        if (aktiviteterMedVariant.isEmpty()) {
            feilHvis(obligatoriskAktivitet) {
                "Finner ikke aktivitet for tiltaksvariant=${tiltaksvariant.name} innenfor perioden $fom - $tom for vilkår=${id.id}"
            }
            return null
        }

        feilHvis(obligatoriskAktivitet && aktiviteterMedVariant.size > 1) {
            "Fant flere aktiviteter med tiltaksvariant=${tiltaksvariant.name} innenfor perioden $fom - $tom for vilkår=${id.id}"
        }

        return aktiviteterMedVariant.singleOrNull()?.tilAktivitetInfoDto()
    }

    private fun VilkårperiodeGlobalId.hentAktivitetInfo(
        behandlingId: BehandlingId,
        obligatoriskAktivitet: Boolean,
    ): AktivitetInfoDto? {
        val aktivitet = vilkårperiodeService.hentAktivitet(this, behandlingId)
        feilHvis(obligatoriskAktivitet && aktivitet == null) {
            "Finner ikke aktivitet med aktivitetId=$this for behandling=$behandlingId"
        }
        return aktivitet?.tilAktivitetInfoDto()
    }
}
