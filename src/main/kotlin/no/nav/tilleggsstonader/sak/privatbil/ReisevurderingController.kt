package no.nav.tilleggsstonader.sak.privatbil

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.ekstern.stønad.DagligReisePrivatBilService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.privatbil.ReisevurderingPrivatBilMapper.tilReisevurderingDto
import no.nav.tilleggsstonader.sak.privatbil.ReisevurderingPrivatBilMapper.tilUkeVurderingDto
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørelisteService
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.EndreAvklartDagRequest
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/kjoreliste"])
@ProtectedWithClaims(issuer = "azuread")
class ReisevurderingController(
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val kjørelisteService: KjørelisteService,
    private val dagligReisePrivatBilService: DagligReisePrivatBilService,
    private val avklartKjørelisteService: AvklartKjørelisteService,
) {
    @GetMapping("{behandlingId}")
    fun hentReisevurderingForBehandling(
        @PathVariable behandlingId: BehandlingId,
    ): List<ReisevurderingPrivatBilDto> {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerLesetilgangTilBehandling(behandlingId)

        val behandling = behandlingService.hentBehandling(behandlingId)

        val kjørelister = kjørelisteService.hentForFagsakId(behandling.fagsakId)
        val reiserIRammevedtak =
            behandling.forrigeIverksatteBehandlingId
                ?.let { dagligReisePrivatBilService.hentRammevedtakForBehandlingId(it)?.reiser }
        val avklarteUker = avklartKjørelisteService.hentAvklarteUkerForBehandling(behandlingId)

        return reiserIRammevedtak?.map { reise ->
            reise.tilReisevurderingDto(avklarteUker = avklarteUker, kjørelister = kjørelister)
        } ?: emptyList()
    }

    @PutMapping("{behandlingId}/{ukeId}")
    fun endreUke(
        @PathVariable behandlingId: BehandlingId,
        @PathVariable ukeId: UUID,
        @RequestBody avklarteDager: List<EndreAvklartDagRequest>,
    ): UkeVurderingDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)

        behandlingService.markerBehandlingSomPåbegyntHvisDenHarStatusOpprettet(behandlingId)

        val oppdatertAvklartUke = avklartKjørelisteService.oppdaterAvklartUke(behandlingId, ukeId, avklarteDager)
        val kjøreliste = kjørelisteService.hentKjøreliste(oppdatertAvklartUke.kjørelisteId)

        return oppdatertAvklartUke.tilUkeVurderingDto(kjøreliste = kjøreliste)
    }
}
