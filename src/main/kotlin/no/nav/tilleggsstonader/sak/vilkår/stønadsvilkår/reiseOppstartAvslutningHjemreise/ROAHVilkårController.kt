package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggRegelstrukturFraVilkårregel.tilRegelstruktur
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.roah.HjemreiseRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.roah.MidlertidigBostedPeriodeRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.roah.OppstartAvslutningRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.VilkårROAHDtoMapper.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.AktivitetMedReiserDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.LagreVilkårROAHDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.RegelstrukturROAHDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.SlettVilkårResultatDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.VilkårROAHDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.tilROAHDto
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
class ROAHVilkårController(
    private val tilgangService: TilgangService,
    private val roahVilkårService: ROAHVilkårService,
) {
    @GetMapping("regler")
    fun regler(): RegelstrukturROAHDto {
        val reglerOppstartAvslutning = OppstartAvslutningRegel().tilRegelstruktur()

        return RegelstrukturROAHDto(
            oppstart = reglerOppstartAvslutning,
            avslutning = reglerOppstartAvslutning,
            hjemreise = HjemreiseRegel().tilRegelstruktur(),
            midlertidigBostedPeriode = MidlertidigBostedPeriodeRegel().tilRegelstruktur(),
        )
    }

    @GetMapping("{behandlingId}")
    fun hentVilkår(
        @PathVariable behandlingId: BehandlingId,
    ): List<AktivitetMedReiserDto> {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerLesetilgangTilBehandling(behandlingId)

        return roahVilkårService.hentVilkårGruppertPåAktivitet(behandlingId).map {
            it.tilDto()
        }
    }

    @PostMapping("{behandlingId}")
    fun opprettVilkår(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody lagreVilkårDto: LagreVilkårROAHDto,
    ): VilkårROAHDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)

        return roahVilkårService
            .opprettNyttVilkår(
                nyttVilkår = lagreVilkårDto.tilDomene(),
                behandlingId = behandlingId,
            ).tilDto()
    }

    @PutMapping("{behandlingId}/{vilkårId}")
    fun oppdaterVilkår(
        @PathVariable behandlingId: BehandlingId,
        @PathVariable vilkårId: VilkårId,
        @RequestBody lagreVilkårDto: LagreVilkårROAHDto,
    ): VilkårROAHDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)

        return roahVilkårService
            .oppdaterVilkår(
                nyttVilkår = lagreVilkårDto.tilDomene(),
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
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.DELETE)

        return roahVilkårService
            .slettVilkår(
                behandlingId = behandlingId,
                vilkårId = vilkårId,
                slettetKommentar = slettVilkårRequestDto.kommentar,
            ).tilROAHDto()
    }
}
