package no.nav.tilleggsstonader.sak.behandling

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.dto.BarnTilRevurderingDto
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingDto
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingTilJournalføringDto
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingsoversiktDto
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.behandling.dto.tilDto
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler.TilordnetSaksbehandlerService
import no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler.dto.tilDto
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/behandling"])
@ProtectedWithClaims(issuer = "azuread")
class BehandlingController(
    private val behandlingService: BehandlingService,
    private val behandlingsoversiktService: BehandlingsoversiktService,
    private val opprettRevurderingService: OpprettRevurderingService,
    private val faktaGrunnlagService: FaktaGrunnlagService,
    private val fagsakService: FagsakService,
    private val henleggService: HenleggService,
    private val tilgangService: TilgangService,
    private val nullstillBehandlingService: NullstillBehandlingService,
    private val tilordnetSaksbehandlerService: TilordnetSaksbehandlerService,
) {
    @GetMapping("{behandlingId}")
    fun hentBehandling(
        @PathVariable behandlingId: BehandlingId,
    ): BehandlingDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        val saksbehandling: Saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val tilordnetSaksbehandler = tilordnetSaksbehandlerService.finnTilordnetSaksbehandler(behandlingId).tilDto()

        if (saksbehandling.status == BehandlingStatus.OPPRETTET) {
            brukerfeilHvisIkke(tilgangService.harTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)) {
                "Behandlingen er ikke påbegynt. En saksbehandler må påbegynne behandlingen før du kan gå inn."
            }
            faktaGrunnlagService.opprettGrunnlagHvisDetIkkeEksisterer(behandlingId)
        }
        return saksbehandling.tilDto(tilordnetSaksbehandler)
    }

    @GetMapping("fagsak-person/{fagsakPersonId}")
    fun hentBehandlingsoversikt(
        @PathVariable fagsakPersonId: FagsakPersonId,
    ): BehandlingsoversiktDto {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)

        return behandlingsoversiktService.hentOversikt(fagsakPersonId)
    }

    @GetMapping("barn-til-revurdering/{fagsakId}")
    fun hentBarnTilRevurdering(
        @PathVariable fagsakId: FagsakId,
    ): BarnTilRevurderingDto {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarSaksbehandlerrolle()
        return opprettRevurderingService.hentBarnTilRevurdering(fagsakId)
    }

    @PostMapping
    fun opprettRevurdering(
        @RequestBody request: OpprettBehandlingDto,
    ): BehandlingId {
        tilgangService.validerTilgangTilFagsak(request.fagsakId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return opprettRevurderingService.opprettRevurdering(request.tilDomene())
    }

    @PostMapping("person")
    fun hentBehandlingerForPersonOgStønadstype(
        @RequestBody identStønadstype: IdentStønadstype,
    ): List<BehandlingTilJournalføringDto> {
        tilgangService.validerTilgangTilStønadstype(
            identStønadstype.ident,
            identStønadstype.stønadstype,
            AuditLoggerEvent.ACCESS,
        )

        return fagsakService.hentBehandlingerForPersonOgStønadstype(
            identStønadstype.ident,
            identStønadstype.stønadstype,
        )
    }

    @PostMapping("{behandlingId}/henlegg")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun henleggBehandling(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody henlagt: HenlagtDto,
    ) {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        henleggService.henleggBehandling(behandlingId, henlagt)
    }

    @GetMapping("/ekstern/{eksternBehandlingId}")
    fun hentBehandling(
        @PathVariable eksternBehandlingId: Long,
    ): BehandlingId {
        val saksbehandling = behandlingService.hentSaksbehandling(eksternBehandlingId)
        tilgangService.validerTilgangTilBehandling(saksbehandling.id, AuditLoggerEvent.ACCESS)
        return saksbehandling.id
    }

    @PostMapping("{behandlingId}/nullstill")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun nullstillBehandling(
        @PathVariable behandlingId: BehandlingId,
    ) {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        nullstillBehandlingService.nullstillBehandling(behandlingService.hentBehandling(behandlingId))
    }
}
