package no.nav.tilleggsstonader.sak.privatbil

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.utils.dato.ukenummer
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.ekstern.stønad.DagligReisePrivatBilService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørelisteService
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtDag
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.TypeAvvikDag
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.TypeAvvikUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UkeStatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UtfyltDagAutomatiskVurdering
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForUke
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/kjoreliste"])
@ProtectedWithClaims(issuer = "azuread")
class PrivatBilController(
    private val behandlingService: BehandlingService,
    private val kjørelisteService: KjørelisteService,
    private val dagligReisePrivatBilService: DagligReisePrivatBilService,
    private val avklartKjørelisteService: AvklartKjørelisteService,
) {
    @GetMapping("{behandlingId}")
    fun hentReisevurderingForBehandling(
        @PathVariable behandlingId: BehandlingId,
    ): List<ReisevurderingPrivatBilDto> {
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
                    reise.uker.map { uke ->
                        val kjørelisteForUke =
                            kjørelister.firstOrNull {
                                it.data.reiseId == reise.reiseId &&
                                    it.inneholderUkenummer(uke.grunnlag.fom.ukenummer())
                            }
                        val avklartUke = avklarteUker.singleOrNull { it.ukenummer == uke.grunnlag.fom.ukenummer() }
                        lagUke(rammeForUke = uke, kjørelisteForUke = kjørelisteForUke, avklartUke = avklartUke)
                    },
            )
        } ?: emptyList()
    }

    private fun lagUke(
        rammeForUke: RammeForUke,
        kjørelisteForUke: Kjøreliste?,
        avklartUke: AvklartKjørtUke?,
    ): UkeVurderingDto {
        val dager =
            (
                0L..ChronoUnit.DAYS.between(
                    rammeForUke.grunnlag.fom,
                    rammeForUke.grunnlag.tom,
                )
            ).map { rammeForUke.grunnlag.fom.plusDays(it) }
                .map { dato ->
                    lagDag(dato, kjørelisteForUke, avklartUke)
                }

        return UkeVurderingDto(
            ukenummer = rammeForUke.grunnlag.fom.ukenummer(),
            fraDato = rammeForUke.grunnlag.fom,
            tilDato = rammeForUke.grunnlag.tom,
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
            dager = dager,
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

private fun AvklartKjørtDag.tilDto() =
    AvklartDag(
        godkjentGjennomførtKjøring = this.godkjentGjennomførtKjøring,
        automatiskVurdering = this.automatiskVurdering,
        avvik = this.avvik,
        begrunnelse = this.begrunnelse,
        parkeringsutgift = this.parkeringsutgift,
    )

data class ReisevurderingPrivatBilDto(
    val reiseId: ReiseId,
    val uker: List<UkeVurderingDto>,
)

data class UkeVurderingDto(
    val ukenummer: Int,
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val status: UkeStatus,
    val avvik: AvvikUke?,
    val behandletDato: LocalDate?,
    val kjørelisteInnsendtDato: LocalDate?, // null hvis kjøreliste ikke er mottatt
    val kjørelisteId: UUID?, // null hvis kjøreliste ikke er mottatt
    val dager: List<DagDto>,
)

data class AvvikUke(
    val typeAvvik: TypeAvvikUke,
    val avviksMelding: String,
)

data class DagDto(
    val dato: LocalDate,
    val ukedag: String, // avklar om faktisk trenger, eller om frontend skal mappe ut fra dag
    val kjørelisteDag: KjørelisteDagDto?,
    val avklartDag: AvklartDag?,
)

data class KjørelisteDagDto(
    val harKjørt: Boolean,
    val parkeringsutgift: Int?,
)

data class AvklartDag(
    val godkjentGjennomførtKjøring: Boolean,
    val automatiskVurdering: UtfyltDagAutomatiskVurdering,
    val avvik: List<TypeAvvikDag>,
    val begrunnelse: String?, // må fylles ut om avvik?
    val parkeringsutgift: Int?,
)
