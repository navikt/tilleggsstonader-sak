package no.nav.tilleggsstonader.sak.privatbil

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.ekstern.stønad.DagligReisePrivatBilService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.privatbil.ReisevurderingPrivatBilMapper.lagUkeVurderingDto
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
        tilgangService.validerHarSaksbehandlerrolle() // TODO: Trengs denne når vi har den over?

        val behandling = behandlingService.hentBehandling(behandlingId)

        val kjørelister = kjørelisteService.hentForFagsakId(behandling.fagsakId)
        val reiserIGjeldendeRammevedtak = dagligReisePrivatBilService.hentRammevedtakForBehandlingId(behandlingId)?.reiser
        val reiserIForrigeRammevedtak =
            behandling.forrigeIverksatteBehandlingId
                ?.let { dagligReisePrivatBilService.hentRammevedtakForBehandlingId(it)?.reiser }
        val avklarteUker = avklartKjørelisteService.hentAvklarteUkerForBehandling(behandlingId)
        val alleReiseIder =
            ((reiserIGjeldendeRammevedtak ?: emptyList()) + (reiserIForrigeRammevedtak ?: emptyList()))
                .map { it.reiseId }
                .distinct()

        return alleReiseIder.map { reiseId ->
            val gjeldendeReise = reiserIGjeldendeRammevedtak?.singleOrNull { it.reiseId == reiseId }
            val forrigeReise = reiserIForrigeRammevedtak?.singleOrNull { it.reiseId == reiseId }
            ReisevurderingPrivatBilMapper.tilReisevurderingDto(
                gjeldendeRammevedtakForReise = gjeldendeReise,
                forrigeRammevedtakForReise = forrigeReise,
                avklarteUker = avklarteUker,
                kjørelister = kjørelister,
            )
        }
    }

    @PutMapping("{behandlingId}/{ukeId}")
    fun endreUke(
        @PathVariable behandlingId: BehandlingId,
        @PathVariable ukeId: UUID,
        @RequestBody avklarteDager: List<EndreAvklartDagRequest>,
    ): UkeVurderingDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle() // TODO: Trengs denne når vi har den over?

        behandlingService.markerBehandlingSomPåbegyntHvisDenHarStatusOpprettet(behandlingId)

        val oppdatertAvklartUke = avklartKjørelisteService.oppdaterAvklartUke(behandlingId, ukeId, avklarteDager)
        val kjøreliste = kjørelisteService.hentKjøreliste(oppdatertAvklartUke.kjørelisteId)

        return lagUkeVurderingDto(
            uke = oppdatertAvklartUke.uke,
            datoer = oppdatertAvklartUke.alleDatoer(),
            avklartUke = oppdatertAvklartUke,
            kjøreliste = kjøreliste,
            erUkeSlettet = false,
        )
    }
}
