package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.utils.dato.ukenummer
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.IdentRequest
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakDto
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakUkeDto
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/ekstern/privat-bil"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class DagligReisePrivatBilController(
    private val dagligReisePrivatBilService: DagligReisePrivatBilService,
    private val eksternApplikasjon: EksternApplikasjon,
) {
    @PostMapping("/rammevedtak")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun hentRammevedtak(
        @RequestBody request: IdentRequest,
    ): List<RammevedtakDto> {
        feilHvisIkke(SikkerhetContext.kallKommerFra(eksternApplikasjon.soknadApi), HttpStatus.UNAUTHORIZED) {
            "Kallet utføres ikke av en autorisert klient"
        }
        return dagligReisePrivatBilService.hentRammevedtaksPrivatBil(request)
            .flatMap { it.tilDto() }
    }
}

private fun RammevedtakPrivatBil.tilDto(): List<RammevedtakDto> =
    reiser.map { reise ->
        RammevedtakDto(
            reiseId = reise.reiseId,
            fom = reise.grunnlag.fom,
            tom = reise.grunnlag.tom,
            reisedagerPerUke = reise.grunnlag.reisedagerPerUke,
            aktivitetsadresse = reise.aktivitetsadresse ?: "Ukjent adresse",
            aktivitetsnavn = "Ukjent aktivitet",
            uker =
                reise.uker.map { uke ->
                    RammevedtakUkeDto(
                        fom = uke.grunnlag.fom,
                        tom = uke.grunnlag.tom,
                        ukeNummer = uke.grunnlag.fom.ukenummer(),
                    )
                },
        )
    }
