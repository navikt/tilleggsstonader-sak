package no.nav.tilleggsstonader.sak.kjøreliste

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.utils.dato.ukenummer
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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
    fun hentKjørelisteForBehandling(
        @PathVariable behandlingId: BehandlingId,
    ): List<KjørelisteDto> {
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
            KjørelisteDto(
                reiseId = reise.reiseId,
                uker =
                    reise.uker.map { uke ->
                        val kjørelisteForUke =
                            kjørelister.firstOrNull {
                                it.data.reiseId == reise.reiseId &&
                                    it.inneholderUkenummer(uke.grunnlag.fom.ukenummer())
                            }
                        UkeDto(
                            ukenummer = uke.grunnlag.fom.ukenummer(),
                            fraDato = uke.grunnlag.fom,
                            tilDato = uke.grunnlag.tom,
                            // TODO: må utvides med flere statuser
                            status = if (kjørelisteForUke == null) UkeStatus.IKKE_MOTTATT_KJØRELISTE else UkeStatus.AVVIK,
                            automatiskVurdering = AutomatiskVurderingUke(
                                typeAvvike = TypeAvvikUke.FLERE_REISEDAGER_ENN_I_RAMMEVEDTAK,
                                avviksMelding = "Dette er egentlig ikke et avvik, bare en test"
                            ),
                            behandletDato = null,
                            kjørelisteInnsendtDato = kjørelisteForUke?.datoMottatt?.toLocalDate(),
                            kjørelisteId = kjørelisteForUke?.id,
                            dager =
                                (
                                    0L..ChronoUnit.DAYS.between(
                                        uke.grunnlag.fom,
                                        uke.grunnlag.tom,
                                    )
                                ).map { uke.grunnlag.fom.plusDays(it) }
                                    .map {
                                        DagDto(
                                            dato = it,
                                            ukedag = it.dayOfWeek.name,
                                            kjørelisteDag =
                                                kjørelisteForUke
                                                    ?.data
                                                    ?.reisedager
                                                    ?.firstOrNull { reisedag -> reisedag.dato == it }
                                                    ?.let { reisedag ->
                                                        KjørelisteDagDto(
                                                            harKjørt = reisedag.harKjørt,
                                                            parkeringsutgift = reisedag.parkeringsutgift,
                                                        )
                                                    },
                                            avklartDag = null,
                                        )
                                    },
                        )
                    },
            )
        } ?: emptyList()
    }
}

data class KjørelisteDto(
    val reiseId: ReiseId,
    val uker: List<UkeDto>,
)

data class UkeDto(
    val ukenummer: Int,
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val status: UkeStatus,
    val automatiskVurdering: AutomatiskVurderingUke,
    val behandletDato: LocalDate?,
    val kjørelisteInnsendtDato: LocalDate?, // null hvis kjøreliste ikke er mottatt
    val kjørelisteId: UUID?, // null hvis kjøreliste ikke er mottatt
    val dager: List<DagDto>,
)

data class AutomatiskVurderingUke(
    val typeAvvike: TypeAvvikUke,
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
