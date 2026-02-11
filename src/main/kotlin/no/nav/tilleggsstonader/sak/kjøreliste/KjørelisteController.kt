package no.nav.tilleggsstonader.sak.kjøreliste

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.utils.dato.ukenummer
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForUke
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
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
class KjørelisteController(
    private val behandlingService: BehandlingService,
    private val kjørelisteService: KjørelisteService,
    private val vedtakService: VedtakService,
) {
    @GetMapping("{behandlingId}")
    fun hentReisevurderingForBehandling(
        @PathVariable behandlingId: BehandlingId,
    ): List<ReisevurderingPrivatBilDto> {
        val behandling = behandlingService.hentBehandling(behandlingId)

        val kjørelister = kjørelisteService.hentForFagsakId(behandling.fagsakId)
        val reiserIRammevedtak =
            behandling.forrigeIverksatteBehandlingId
                ?.let {
                    vedtakService
                        .hentVedtak<InnvilgelseEllerOpphørDagligReise>(behandling.forrigeIverksatteBehandlingId)
                }?.data
                ?.rammevedtakPrivatBil
                ?.reiser

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
                        lagUke(rammeForUke = uke, kjørelisteForUke = kjørelisteForUke)
                    },
            )
        } ?: emptyList()
    }

    private fun lagUke(
        rammeForUke: RammeForUke,
        kjørelisteForUke: Kjøreliste?,
    ): UkeVurderingDto {
        val dager =
            (
                0L..ChronoUnit.DAYS.between(
                    rammeForUke.grunnlag.fom,
                    rammeForUke.grunnlag.tom,
                )
            ).map { rammeForUke.grunnlag.fom.plusDays(it) }
                .map { dato ->
                    lagDag(dato, kjørelisteForUke)
                }

        val avvik =
            if (!vurderAntallDagerInnenforRamme(dager, rammeForUke)) {
                AvvikUke(
                    typeAvvik = TypeAvvikUke.FLERE_REISEDAGER_ENN_I_RAMMEVEDTAK,
                    avviksMelding = "Dette er egentlig ikke et avvik, bare en test",
                )
            } else {
                null
            }

        return UkeVurderingDto(
            ukenummer = rammeForUke.grunnlag.fom.ukenummer(),
            fraDato = rammeForUke.grunnlag.fom,
            tilDato = rammeForUke.grunnlag.tom,
            status = utledStatusForUke(kjørelisteForUke, dager, avvik),
            avvik = avvik,
            behandletDato = null,
            kjørelisteInnsendtDato = kjørelisteForUke?.datoMottatt?.toLocalDate(),
            kjørelisteId = kjørelisteForUke?.id,
            dager = dager,
        )
    }

    private fun vurderAntallDagerInnenforRamme(
        dager: List<DagDto>,
        rammeForUke: RammeForUke,
    ): Boolean {
        val antallDagerMedUtbetaling = dager.filter { it.kjørelisteDag?.harKjørt == true }.size

        return antallDagerMedUtbetaling <= rammeForUke.grunnlag.maksAntallDagerSomKanDekkes
    }

    private fun utledStatusForUke(
        kjørelisteForUke: Kjøreliste?,
        dager: List<DagDto>,
        avvikUke: AvvikUke?,
    ): UkeStatus {
        if (kjørelisteForUke == null) return UkeStatus.IKKE_MOTTATT_KJØRELISTE

        if (avvikUke != null) return UkeStatus.AVVIK

        val automatiskeVurderingForDager = dager.map { it.avklartDag?.automatiskVurdering }.toSet()

        // Usikker på hvordan denne sjekken dersom vi tillater at man kan sende inn en halv uke, som vil gi
        // en dag hvor avklartDag = null og dermed ikke har en automatisk vurdering
        if (automatiskeVurderingForDager.size == 1 && automatiskeVurderingForDager.single() == UtfyltDagAutomatiskVurdering.OK) {
            return UkeStatus.OK_AUTOMATISK
        }

        return UkeStatus.AVVIK
    }

    private fun lagDag(
        dato: LocalDate,
        kjørelisteForUke: Kjøreliste?,
    ): DagDto {
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
            avklartDag = lagAvklartDag(dato = dato, kjørelisteForDag = kjørelisteForDag),
        )
    }

    private fun lagAvklartDag(
        dato: LocalDate,
        kjørelisteForDag: KjørelisteDag?,
    ): AvklartDag? {
        if (kjørelisteForDag == null) return null

        val avvik =
            utledAvvik(
                dato = dato,
                kjørelisteForDag = kjørelisteForDag,
            )

        return AvklartDag(
            godkjentGjennomførtKjøring = kjørelisteForDag.harKjørt,
            automatiskVurdering = if (avvik.isEmpty()) UtfyltDagAutomatiskVurdering.OK else UtfyltDagAutomatiskVurdering.AVVIK,
            avvik = avvik,
            begrunnelse = null,
            parkeringsutgift = kjørelisteForDag.parkeringsutgift,
        )
    }

    private fun utledAvvik(
        dato: LocalDate,
        kjørelisteForDag: KjørelisteDag,
    ): List<TypeAvvikDag> {
        if (!kjørelisteForDag.harKjørt) return emptyList()

        val erHelg = dato.dayOfWeek == DayOfWeek.SATURDAY || dato.dayOfWeek == DayOfWeek.SUNDAY
        return listOfNotNull(
            TypeAvvikDag.FOR_HØY_PARKERINGSUTGIFT.takeIf {
                kjørelisteForDag.parkeringsutgift != null &&
                    kjørelisteForDag.parkeringsutgift > 100
            },
            TypeAvvikDag.HELLIDAG_ELLER_HELG.takeIf { erHelg },
        )
    }
}

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

enum class UkeStatus {
    OK_AUTOMATISK, // brukes hvis automatisk godkjent
    OK_MANUELT, // brukes hvis saksbehandler godtar avvik
    AVVIK, // parkeringsutgifter/for mange dager etc. saksbehandler må ta stilling til uka
    IKKE_MOTTATT_KJØRELISTE,
}

enum class UtfyltDagAutomatiskVurdering {
    OK,
    AVVIK,
}

enum class TypeAvvikDag {
    FOR_HØY_PARKERINGSUTGIFT,
    HELLIDAG_ELLER_HELG,
}

enum class TypeAvvikUke {
    FLERE_REISEDAGER_ENN_I_RAMMEVEDTAK,
}
