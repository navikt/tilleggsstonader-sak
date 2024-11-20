package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.aktivitet

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeResponse
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/vilkarperiode2/aktivitet"])
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

    @PostMapping("{id}")
    fun oppdaterAktivitet(
        @PathVariable("id") id: UUID,
        @RequestBody aktivitet: LagreAktivitet,
    ): LagreVilkårperiodeResponse {
        tilgangService.validerTilgangTilBehandling(aktivitet.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        val periode = aktivitetService.oppdaterAktivitet(id, aktivitet)
        return vilkårperiodeService.validerOgLagResponse(behandlingId = periode.behandlingId, periode = periode)
    }
}
