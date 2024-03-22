package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OppdaterVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.rest.OppdaterVilkårsvurderingRestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.rest.VilkårRestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.rest.VilkårsvurderingerJson
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.rest.tilDelvilkårDtoer
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.rest.tilRestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregler
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/vilkar"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VilkårController(
    private val vilkårService: VilkårService,
    private val tilgangService: TilgangService,
    // private val gjenbrukVilkårService: GjenbrukVilkårService,
) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @GetMapping("regler")
    @Deprecated("Brukes ikke lenger av frontend, kan fjernes.")
    fun hentRegler(): Vilkårsregler {
        return Vilkårsregler.ALLE_VILKÅRSREGLER
    }

    @PostMapping
    @Deprecated("Erstattet av oppdaterVilkårsvurdering", ReplaceWith("oppdaterVilkårsvurdering()"))
    fun oppdaterVilkår(@RequestBody svarPåVilkårDto: SvarPåVilkårDto): VilkårDto {
        tilgangService.validerTilgangTilBehandling(svarPåVilkårDto.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        try {
            return vilkårService.oppdaterVilkår(svarPåVilkårDto)
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

    @PostMapping("oppdater")
    fun oppdaterVilkårsvurdering(@RequestBody vilkårsvurdering: OppdaterVilkårsvurderingRestDto): VilkårRestDto {
        val vilkårId = vilkårsvurdering.id
        val behandlingId = vilkårsvurdering.behandlingId
        val vurderinger = vilkårsvurdering.vurdering.tilDelvilkårDtoer()

        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        try {
            return vilkårService.oppdaterVilkårsvurdering(vilkårId, behandlingId, vurderinger).tilRestDto()
        } catch (e: Exception) {
            val delvilkårJson = objectMapper.writeValueAsString(vilkårsvurdering)
            secureLogger.warn(
                "id=${vilkårsvurdering.id}" +
                        " behandlingId=${vilkårsvurdering.behandlingId}" +
                        " svar=$delvilkårJson",
            )
            throw e
        }
    }

    @PostMapping("nullstill")
    fun nullstillVilkår(@RequestBody request: OppdaterVilkårDto): VilkårDto {
        tilgangService.validerTilgangTilBehandling(request.behandlingId, AuditLoggerEvent.DELETE)
        tilgangService.validerHarSaksbehandlerrolle()
        return vilkårService.nullstillVilkår(request)
    }

    @PostMapping("ikkevurder")
    fun settVilkårTilSkalIkkeVurderes(@RequestBody request: OppdaterVilkårDto): VilkårDto {
        tilgangService.validerTilgangTilBehandling(request.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        return vilkårService.settVilkårTilSkalIkkeVurderes(request)
    }

    @GetMapping("{behandlingId}")
    @Deprecated("Erstattet av getVilkårsvurdering()")
    fun getVilkår(@PathVariable behandlingId: UUID): VilkårsvurderingDto {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return vilkårService.hentOpprettEllerOppdaterVilkårsvurdering(behandlingId)
    }

    @GetMapping("{behandlingId}/vurderinger")
    fun getVilkårsvurdering(@PathVariable behandlingId: UUID): VilkårsvurderingerJson {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)

        val vilkårsvurderinger = vilkårService.hentOpprettEllerOppdaterVilkårsvurdering(behandlingId)

        return VilkårsvurderingerJson(
            vilkårsett = vilkårsvurderinger.vilkårsett.map { it.tilRestDto() },
            grunnlag = vilkårsvurderinger.grunnlag,
        )
    }

    @GetMapping("{behandlingId}/oppdater")
    fun oppdaterRegisterdata(@PathVariable behandlingId: UUID): VilkårsvurderingDto {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        return vilkårService.oppdaterGrunnlagsdataOgHentEllerOpprettVurderinger(behandlingId)
    }

    /*@PostMapping("gjenbruk")
    fun gjenbrukVilkår(@RequestBody request: GjenbrukVilkårDto): VilkårDto {
        tilgangService.validerTilgangTilBehandling(request.kopierBehandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerTilgangTilBehandling(request.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        gjenbrukVilkårService.gjenbrukInngangsvilkårVurderinger(request.behandlingId, request.kopierBehandlingId)
        return vurderingService.hentEllerOpprettVurderinger(request.behandlingId)
    }
     */
}
