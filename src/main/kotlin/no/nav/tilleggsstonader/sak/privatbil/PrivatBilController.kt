package no.nav.tilleggsstonader.sak.privatbil

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.libs.utils.dato.alleDatoerGruppertPåUke
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.ekstern.stønad.DagligReisePrivatBilService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørelisteService
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.EndreAvklartDagRequest
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UkeStatus
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.tilDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/kjoreliste"])
@ProtectedWithClaims(issuer = "azuread")
class PrivatBilController(
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
        val reiserIRammevedtak =
            behandling.forrigeIverksatteBehandlingId
                ?.let { dagligReisePrivatBilService.hentRammevedtakForBehandlingId(it)?.reiser }
        val avklarteUker = avklartKjørelisteService.hentAvklarteUkerForBehandling(behandlingId)

        return reiserIRammevedtak?.map { reise ->
            ReisevurderingPrivatBilDto(
                reiseId = reise.reiseId,
                uker =
                    reise.grunnlag
                        .alleDatoerGruppertPåUke()
                        .map { (uke, datoer) ->
                            val avklartUke =
                                avklarteUker.singleOrNull { it.reiseId == reise.reiseId && it.uke == uke }

                            val kjørelisteForUke =
                                avklartUke?.let {
                                    kjørelister.firstOrNull {
                                        it.id == avklartUke.kjørelisteId
                                    }
                                }

                            lagUke(uke = uke, datoer = datoer, kjørelisteForUke = kjørelisteForUke, avklartUke = avklartUke)
                        },
                rammevedtak = reise.tilDto(),
            )
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
        tilgangService.validerHarSaksbehandlerrolle() // TODO: Trengs denne når vi har den over?

        behandlingService.markerBehandlingSomPåbegyntHvisDenHarStatusOpprettet(behandlingId)

        val oppdatertAvklartUke = avklartKjørelisteService.oppdaterAvklartUke(behandlingId, ukeId, avklarteDager)
        val kjøreliste = kjørelisteService.hentKjøreliste(oppdatertAvklartUke.kjørelisteId)

        return lagUke(
            uke = oppdatertAvklartUke.uke,
            datoer = oppdatertAvklartUke.alleDatoer(),
            kjørelisteForUke = kjøreliste,
            avklartUke = oppdatertAvklartUke,
        )
    }

    private fun lagUke(
        uke: UkeIÅr,
        datoer: List<LocalDate>,
        kjørelisteForUke: Kjøreliste?,
        avklartUke: AvklartKjørtUke?,
    ): UkeVurderingDto {
        val dager =
            datoer.map { dato ->
                lagDag(dato, kjørelisteForUke, avklartUke)
            }

        return UkeVurderingDto(
            ukenummer = uke.ukenummer,
            fraDato = datoer.min(),
            tilDato = datoer.max(),
            status = avklartUke?.status ?: UkeStatus.IKKE_MOTTATT_KJØRELISTE,
            avvik =
                avklartUke?.typeAvvik?.let {
                    AvvikUke(
                        typeAvvik = it,
                        avviksMelding = "Dette er egentlig ikke et avvik, bare en test",
                    )
                },
            behandletDato = avklartUke?.behandletDato,
            kjørelisteInnsendtDato = kjørelisteForUke?.datoMottatt?.toLocalDate(),
            kjørelisteId = kjørelisteForUke?.id,
            avklartUkeId = avklartUke?.id,
            dager = dager,
            avklartKjørtUkeStatus = avklartUke?.avklartKjørtUkeStatus,
        )
    }

    private fun lagDag(
        dato: LocalDate,
        kjørelisteForUke: Kjøreliste?,
        avklartUke: AvklartKjørtUke?,
    ): DagDto {
        val avklartDag = avklartUke?.dager?.singleOrNull { it.dato == dato }

        // TODO: Vurder å kaste feil dersom denne er null
        // Hvis den eksisterer for en uke så burde alle dager eksistere?
        val kjørelisteForDag =
            kjørelisteForUke
                ?.data
                ?.reisedager
                ?.firstOrNull { reisedag -> reisedag.dato == dato }

        return DagDto(
            dato = dato,
            ukedag = dato.dayOfWeek.name,
            kjørelisteDag =
                kjørelisteForDag?.let { reisedag ->
                    KjørelisteDagDto(
                        harKjørt = reisedag.harKjørt,
                        parkeringsutgift = reisedag.parkeringsutgift,
                    )
                },
            avklartDag = avklartDag?.tilDto(),
        )
    }
}
