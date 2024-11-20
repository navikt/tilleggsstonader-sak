package no.nav.tilleggsstonader.sak.opplysninger.aktivitet

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/aktivitet"])
@ProtectedWithClaims(issuer = "azuread")
class RegisterAktivitetController(
    private val tilgangService: TilgangService,
    private val registerAktivitetService: RegisterAktivitetService,
    private val behandlingService: BehandlingService,
) {

    @GetMapping("temp/{fagsakPersonId}")
    fun hentAktiviteter(
        @PathVariable fagsakPersonId: FagsakPersonId,
    ): RegisterAktiviteterDto {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return registerAktivitetService.hentAktiviteterMedPerioder(fagsakPersonId)
    }

    @Deprecated("Byttes ut med hentAktiviteter, fordi vi trenger informasjon om for hvilken periode vi henter ut data.")
    @GetMapping("{fagsakPersonId}")
    fun hentAktiviteterDeprecated(
        @PathVariable fagsakPersonId: FagsakPersonId,
    ): List<AktivitetArenaDto> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return registerAktivitetService.hentAktiviteter(fagsakPersonId)
    }

    @Deprecated("Skal fjernes. aktivitet skal hentes med vilkårperioder") // TODO: Fjern når aktivitet hentes med vilkårperioder
    @GetMapping("/behandling/{behandlingId}")
    fun hentAktivitetForBehandling(
        @PathVariable behandlingId: BehandlingId,
    ): List<AktivitetArenaDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        return registerAktivitetService.hentAktiviteter(saksbehandling.fagsakPersonId)
    }
}
