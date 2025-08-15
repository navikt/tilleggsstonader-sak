package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OppdaterVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårResponse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregler
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/vilkar"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VilkårController(
    private val vilkårService: VilkårService,
    private val tilgangService: TilgangService,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @GetMapping("regler")
    fun hentRegler(): Vilkårsregler = Vilkårsregler.ALLE_VILKÅRSREGLER

    @PostMapping
    fun oppdaterVilkår(
        @RequestBody svarPåVilkårDto: SvarPåVilkårDto,
    ): VilkårDto {
        tilgangService.settBehandlingsdetaljerForRequest(svarPåVilkårDto.behandlingId)
        tilgangService.validerTilgangTilBehandling(svarPåVilkårDto.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        try {
            return vilkårService.oppdaterVilkår(svarPåVilkårDto).tilDto()
        } catch (e: Exception) {
            val delvilkårJson = objectMapper.writeValueAsString(svarPåVilkårDto.delvilkårsett)
            secureLogger.warn(
                "id=${svarPåVilkårDto.id}" +
                    " behandlingId=${svarPåVilkårDto.behandlingId}" +
                    " svar=$delvilkårJson",
            )
            throw e
        }
    }

    @PostMapping("opprett")
    fun opprettVilkår(
        @RequestBody opprettVilkårDto: OpprettVilkårDto,
    ): VilkårDto {
        tilgangService.settBehandlingsdetaljerForRequest(opprettVilkårDto.behandlingId)
        tilgangService.validerTilgangTilBehandling(opprettVilkårDto.behandlingId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return vilkårService.opprettNyttVilkår(opprettVilkårDto).tilDto()
    }

    @DeleteMapping
    fun slettVilkår(
        @RequestBody request: SlettVilkårRequest,
    ): SlettVilkårResponse {
        tilgangService.settBehandlingsdetaljerForRequest(request.behandlingId)
        tilgangService.validerTilgangTilBehandling(request.behandlingId, AuditLoggerEvent.DELETE)
        tilgangService.validerHarSaksbehandlerrolle()

        return vilkårService.slettVilkår(request).tilDto()
    }

    @PostMapping("ikkevurder")
    fun settVilkårTilSkalIkkeVurderes(
        @RequestBody request: OppdaterVilkårDto,
    ): VilkårDto {
        tilgangService.settBehandlingsdetaljerForRequest(request.behandlingId)
        tilgangService.validerTilgangTilBehandling(request.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        return vilkårService.settVilkårTilSkalIkkeVurderes(request)
    }

    @GetMapping("{behandlingId}")
    fun getVilkår(
        @PathVariable behandlingId: BehandlingId,
    ): VilkårsvurderingDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return VilkårsvurderingDto(vilkårService.hentVilkår(behandlingId).map { it.tilDto() })
    }
}
