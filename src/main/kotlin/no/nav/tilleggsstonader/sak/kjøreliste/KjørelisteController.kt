package no.nav.tilleggsstonader.sak.kjøreliste

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

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
        return kjørelisteService.hentForFagsakId(behandling.fagsakId).map {
            KjørelisteDto(
                reiseId = it.data.reiseId,
                uker =
                    listOf(
                        KjørelisteUkeDto(
                            ukeNummer = 1,
                            reisedager =
                                it.data.reisedager.map { reisedag ->
                                    KjørelisteDagDto(
                                        dato = reisedag.dato,
                                        harKjørt = reisedag.harKjørt,
                                        parkeringsutgift = reisedag.parkeringsutgift,
                                    )
                                },
                        ),
                    ),
            )
        }
    }
}

data class KjørelisteDto(
    val reiseId: ReiseId,
    val uker: List<KjørelisteUkeDto>,
)

data class KjørelisteUkeDto(
    val ukeNummer: Int,
    val reisedager: List<KjørelisteDagDto>,
)

data class KjørelisteDagDto(
    val dato: LocalDate,
    val harKjørt: Boolean,
    val parkeringsutgift: Int?,
)
