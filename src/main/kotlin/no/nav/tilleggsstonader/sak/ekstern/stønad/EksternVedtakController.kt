package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.IdentRequest
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.VedtaksinformasjonTilsynBarnDto
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/ekstern/vedtak"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class EksternVedtakController(
    private val eksternVedtakService: EksternVedtakService,
) {
    @PostMapping("tilsyn-barn")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun hentVedtaksinformasjon(
        @RequestBody request: IdentRequest,
    ): VedtaksinformasjonTilsynBarnDto {
        val gyldigKlient =
            SikkerhetContext.kallKommerFra(
                EksternApplikasjon.BIDRAG_GRUNNLAG,
                EksternApplikasjon.BIDRAG_GRUNNLAG_FEATURE,
            )
        feilHvisIkke(gyldigKlient, HttpStatus.UNAUTHORIZED) {
            "Kallet utføres ikke av en autorisert klient"
        }
        return eksternVedtakService.hentVedtaksinformasjonTilsynBarn(request)
    }
}
