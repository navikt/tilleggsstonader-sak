package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeResponse
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping(path = ["/api/vilkårperiode/aktivitet"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class AktivitetController(
    private val tilgangService: TilgangService,
    private val aktivitetService: AktivitetService,
    private val vilkårperiodeService: VilkårperiodeService,
) {
    @PostMapping
    fun opprettAktivitet(
        @RequestBody aktivitet: LagreAktivitet,
    ): LagreVilkårperiodeResponse {
        tilgangService.validerHarSaksbehandlerrolle()

        val periode = aktivitetService.opprettAktivitet(aktivitet)
        return vilkårperiodeService.validerOgLagResponse(behandlingId = periode.behandlingId, periode = periode)
    }

//    @PostMapping("{id}")
//    fun oppdaterPeriode(
//        @PathVariable("id") id: UUID,
//        @RequestBody aktivitet: LagreAktivitet,
//    ): LagreVilkårperiodeResponse {
//        tilgangService.validerTilgangTilBehandling(aktivitet.behandlingId, AuditLoggerEvent.UPDATE)
//        tilgangService.validerHarSaksbehandlerrolle()
//
//        val periode = aktivitetService.oppdaterVilkårperiode(id, aktivitet)
//        return aktivitetService.validerOgLagResponse(behandlingId = periode.behandlingId, periode = periode)
//    }
//
//    @PostMapping("{id}")
//    fun oppdaterPeriode(
//        @PathVariable("id") id: UUID,
//        @RequestBody aktivitet: LagreAktivitet,
//    ): LagreVilkårperiodeResponse {
//        tilgangService.validerTilgangTilBehandling(aktivitet.behandlingId, AuditLoggerEvent.UPDATE)
//        tilgangService.validerHarSaksbehandlerrolle()
//
//        val periode = aktivitetService.oppdaterVilkårperiode(id, aktivitet)
//        return aktivitetService.validerOgLagResponse(behandlingId = periode.behandlingId, periode = periode)
//    }
}

data class LagreAktivitet(
    val behandlingId: BehandlingId,
    val type: AktivitetType,
    val fom: LocalDate,
    val tom: LocalDate,
    val aktivitetsdager: Int? = null,
    val prosent: Int? = null,
    val svarLønnet: SvarJaNei? = null,
    val svarHarUtgifter: SvarJaNei? = null,
    val begrunnelse: String? = null,
    val kildeId: String? = null,
)
