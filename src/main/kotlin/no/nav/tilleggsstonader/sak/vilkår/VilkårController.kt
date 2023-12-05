package no.nav.tilleggsstonader.sak.vilkår

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.dto.OppdaterVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.OpprettVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.dto.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregler
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
    private val vilkårStegService: VilkårStegService,
    private val tilgangService: TilgangService,
    // private val gjenbrukVilkårService: GjenbrukVilkårService,
) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @GetMapping("regler")
    fun hentRegler(): Vilkårsregler {
        return Vilkårsregler.ALLE_VILKÅRSREGLER
    }

    @PostMapping
    fun oppdaterVilkår(@RequestBody svarPåVilkårDto: SvarPåVilkårDto): VilkårDto {
        tilgangService.validerTilgangTilBehandling(svarPåVilkårDto.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        try {
            return vilkårStegService.oppdaterVilkår(svarPåVilkårDto)
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

    @PostMapping("nullstill")
    fun nullstillVilkår(@RequestBody request: OppdaterVilkårDto): VilkårDto {
        tilgangService.validerTilgangTilBehandling(request.behandlingId, AuditLoggerEvent.DELETE)
        tilgangService.validerHarSaksbehandlerrolle()
        return vilkårStegService.nullstillVilkår(request)
    }

    @PostMapping("ikkevurder")
    fun settVilkårTilSkalIkkeVurderes(@RequestBody request: OppdaterVilkårDto): VilkårDto {
        tilgangService.validerTilgangTilBehandling(request.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        return vilkårStegService.settVilkårTilSkalIkkeVurderes(request)
    }

    @GetMapping("{behandlingId}")
    fun getVilkår(@PathVariable behandlingId: UUID): VilkårsvurderingDto {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return vilkårService.hentOpprettEllerOppdaterVilkårsvurdering(behandlingId)
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

    @GetMapping("{behandlingId}/perioder")
    fun hentMålgrupper(@PathVariable behandlingId: UUID): Vilkårperioder {
        return vilkårService.hentVilkårperioder(behandlingId)
    }

    @PostMapping("{behandlingId}/perioder")
    fun opprettVilkårMedPeriode(
        @PathVariable behandlingId: UUID,
        @RequestBody opprettVilkårperiode: OpprettVilkårperiode,
    ): VilkårperiodeDto {
        return vilkårService.opprettVilkårperiode(behandlingId, opprettVilkårperiode)
    }
}
