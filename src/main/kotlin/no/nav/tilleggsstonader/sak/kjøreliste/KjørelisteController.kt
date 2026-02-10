package no.nav.tilleggsstonader.sak.kjøreliste

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.utils.dato.ukenummer
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/kjoreliste"])
@ProtectedWithClaims(issuer = "azuread")
class KjørelisteController(
    private val behandlingService: BehandlingService,
    private val kjørelisteService: KjørelisteService,
) {
    @GetMapping("{behandlingId}")
    fun hentKjørelisteForBehandling(
        @PathVariable behandlingId: BehandlingId,
    ): List<KjørelisteDto> {
        val behandling = behandlingService.hentBehandling(behandlingId)

        return kjørelisteService.hentForFagsakId(behandling.fagsakId).map { kjøreliste ->
            KjørelisteDto(
                reiseId = kjøreliste.data.reiseId,
                innsendteUker =
                    kjøreliste.data.reisedager
                        .map { reisedag ->
                            KjørelisteDagDto(
                                dato = reisedag.dato,
                                harKjørt = reisedag.harKjørt,
                                parkeringsutgift = reisedag.parkeringsutgift,
                            )
                        }.groupBy {
                            it.dato.ukenummer()
                        }.map { (uke, reisedagerIUke) ->
                            KjørelisteUkeDto(
                                ukeNummer = uke,
                                reisedager = reisedagerIUke,
                            )
                        },
            )
        }
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
    val avviksbegrunnelse: AvviksbegrunnelseUke?,
    val avviksMelding: String?,
    val behandletDato: LocalDate?,
    val kjørelisteInnsendtDato: LocalDate?, // null hvis kjøreliste ikke er mottatt
    val kjørelisteId: UUID?, // null hvis kjøreliste ikke er mottatt
    val dag: List<DagDto>
)

data class DagDto(
    val dato: LocalDate,
    val ukedag: String, // avklar om faktisk trenger, eller om frontend skal mappe ut fra dag
    val kjørelisteDag: KjørelisteDagDto?,
    val avklartDag: AvklartDag?
)

data class KjørelisteDagDto(
    val harKjørt: Boolean,
    val parkeringsutgift: Int?,
)

data class AvklartDag(
    val resultat: UtfyltDagResultat,
    val automatiskVurdering: UtfyltDagAutomatiskVurdering,
    val avviksbegrunnelse: AvviksbegrunnelseDag,
    val begrunnelse: String?, // må fylles ut om avvik?
    val parkeringsutgift: Int?,
)

enum class UkeStatus {
    OK_AUTOMATISK, // brukes hvis automatisk godkjent
    OK_MANUELT, // brukes hvis saksbehandler godtar avvik
    AVVIK, // parkeringsutgifter/for mange dager etc. saksbehandler må ta stilling til uka
    KORRIGERT, // saksbehandler har endret innsendte dager
    IKKE_MOTTATT_KJØRELISTE,
}

enum class UtfyltDagResultat {
    UTBETALING,
    IKKE_UTBETALING
}

enum class UtfyltDagAutomatiskVurdering {
    OK,
    AVVIK
}

enum class AvviksbegrunnelseDag {
    FOR_HØY_PARKERINGSUTGIFT,
    HELLIDAG_ELLER_HELG,
}

enum class AvviksbegrunnelseUke {
    FLERE_REISEDAGER_ENN_I_RAMMEVEDTAK
}