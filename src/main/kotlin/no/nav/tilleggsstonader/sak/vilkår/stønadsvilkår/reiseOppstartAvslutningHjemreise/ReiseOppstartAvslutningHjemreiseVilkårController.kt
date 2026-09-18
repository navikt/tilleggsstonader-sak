package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.AktivitetPåFaktaDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilAktivitetPåFaktaDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggRegelstrukturFraVilkårregel.tilRegelstruktur
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.ReiseOppstartAvslutningHjemreiseRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.VilkårReiseOppstartAvslutningHjemreiseDtoMapper.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.VilkårReiseOppstartAvslutningHjemreiseMapper.mapTilVilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.AktivitetMedReiserDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.LagreVilkårReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.SlettVilkårResultatDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.VilkårReiseOppstartAvslutningHjemreiseDto
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
@RequestMapping(path = ["/api/vilkar/reise-oppstart-avslutning-hjemreise"])
@ProtectedWithClaims(issuer = "azuread")
class ReiseOppstartAvslutningHjemreiseVilkårController(
    private val tilgangService: TilgangService,
    private val reiseOppstartAvslutningHjemreiseVilkårService: ReiseOppstartAvslutningHjemreiseVilkårService,
    private val vilkårperiodeService: VilkårperiodeService,
) {
    @GetMapping("regler")
    fun regler(): RegelstrukturDto = ReiseOppstartAvslutningHjemreiseRegel().tilRegelstruktur()

    @GetMapping("{behandlingId}")
    fun hentVilkår(
        @PathVariable behandlingId: BehandlingId,
    ): List<AktivitetMedReiserDto> {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerLesetilgangTilBehandling(behandlingId)

        return reiseOppstartAvslutningHjemreiseVilkårService.hentVilkårGruppertPåAktivitet(behandlingId).map {
            it.tilDto()
        }
    }

    @PostMapping("{behandlingId}")
    fun opprettVilkår(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody lagreVilkårDto: LagreVilkårReiseOppstartAvslutningHjemreiseDto,
    ): VilkårReiseOppstartAvslutningHjemreiseDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)

        return reiseOppstartAvslutningHjemreiseVilkårService
            .opprettNyttVilkår(
                nyttVilkår = lagreVilkårDto.tilDomain(),
                behandlingId = behandlingId,
            ).tilDtoMedAktivitet(behandlingId)
    }

    @PutMapping("{behandlingId}/{vilkårId}")
    fun oppdaterVilkår(
        @PathVariable behandlingId: BehandlingId,
        @PathVariable vilkårId: VilkårId,
        @RequestBody lagreVilkårDto: LagreVilkårReiseOppstartAvslutningHjemreiseDto,
    ): VilkårReiseOppstartAvslutningHjemreiseDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)

        return reiseOppstartAvslutningHjemreiseVilkårService
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
            reiseOppstartAvslutningHjemreiseVilkårService
                .slettVilkår(
                    behandlingId = behandlingId,
                    vilkårId = vilkårId,
                    slettetKommentar = slettVilkårRequestDto.kommentar,
                )

        return SlettVilkårResultatDto(
            slettetPermanent = slettetVilkårResultat.slettetPermanent,
            vilkår =
                slettetVilkårResultat.vilkår
                    .mapTilVilkårReiseOppstartAvslutningHjemreise()
                    .tilDtoMedAktivitet(behandlingId),
        )
    }

    private fun VilkårReiseOppstartAvslutningHjemreise.tilDtoMedAktivitet(
        behandlingId: BehandlingId,
    ): VilkårReiseOppstartAvslutningHjemreiseDto {
        val aktivitet = fakta.aktivitetPåFakta(behandlingId)
        return tilDto(aktivitet = aktivitet)
    }

    private fun FaktaReiseOppstartAvslutningHjemreise.aktivitetPåFakta(behandlingId: BehandlingId): AktivitetPåFaktaDto? =
        when (this) {
            is FaktaOffentligTransport ->
                this.aktivitetId?.let { vilkårperiodeService.hentAktivitet(it, behandlingId) }?.tilAktivitetPåFaktaDto()

            is FaktaPrivatBil ->
                vilkårperiodeService
                    .hentAktivitet(this.aktivitetId, behandlingId)
                    ?.tilAktivitetPåFaktaDto()
                    ?: error("Finner ikke aktivitet for privat bil med aktivitetId=${this.aktivitetId}")

            is FaktaUbestemtType -> null
        }
}
