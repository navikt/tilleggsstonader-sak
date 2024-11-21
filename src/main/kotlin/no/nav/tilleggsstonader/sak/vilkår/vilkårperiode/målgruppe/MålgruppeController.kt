package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.målgruppe

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
@RequestMapping(path = ["/api/målgruppe"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class MålgruppeController(
    private val tilgangService: TilgangService,
    private val målgruppeService: MålgruppeService,
    private val vilkårperiodeService: VilkårperiodeService,
) {
    @PostMapping
    fun opprettMålgruppe(
        @RequestBody aktivitet: LagreMålgruppe,
    ): LagreVilkårperiodeResponse {
        tilgangService.validerHarSaksbehandlerrolle()

        val periode = målgruppeService.opprettMålgruppe(aktivitet)
        return vilkårperiodeService.validerOgLagResponse(behandlingId = periode.behandlingId, periode = periode)
    }

    @PostMapping("{id}")
    fun oppdaterMålgruppe(
        @PathVariable("id") id: UUID,
        @RequestBody aktivitet: LagreMålgruppe,
    ): LagreVilkårperiodeResponse {
        tilgangService.validerTilgangTilBehandling(aktivitet.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        val periode = målgruppeService.oppdaterMålgruppe(id, aktivitet)
        return vilkårperiodeService.validerOgLagResponse(behandlingId = periode.behandlingId, periode = periode)
    }
}
