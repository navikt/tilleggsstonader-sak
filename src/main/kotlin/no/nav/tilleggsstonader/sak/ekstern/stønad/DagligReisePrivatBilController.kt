package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.sikkerhet.EksternBrukerUtils
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakDto
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/ekstern/privat-bil"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class DagligReisePrivatBilController(
    private val dagligReisePrivatBilService: DagligReisePrivatBilService,
) {
    @GetMapping("/rammevedtak")
    @ProtectedWithClaims(issuer = EksternBrukerUtils.ISSUER_TOKENX, claimMap = ["acr=Level4"])
    fun hentRammevedtak(): List<RammevedtakDto> =
        dagligReisePrivatBilService.hentRammevedtaksPrivatBil(EksternBrukerUtils.hentFnrFraToken())
}
