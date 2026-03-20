package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.søknad.RammevedtakDto
import no.nav.tilleggsstonader.libs.sikkerhet.EksternBrukerUtils
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteService
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
    private val kjørelisteService: KjørelisteService,
    private val fagsakService: FagsakService,
) {
    @GetMapping("/rammevedtak")
    @ProtectedWithClaims(issuer = EksternBrukerUtils.ISSUER_TOKENX, claimMap = ["acr=Level4"])
    fun hentRammevedtak(): List<RammevedtakDto> {
        val ident = EksternBrukerUtils.hentFnrFraToken()

        val kjørelister =
            fagsakService
                .finnFagsaker(setOf(ident))
                .flatMap { kjørelisteService.hentForFagsakId(it.id) }

        val rammevedtak = dagligReisePrivatBilService.hentRammevedtaksPrivatBil(ident)
        val kjøreliste = kjørelister.groupBy { it.data.reiseId }

        return rammevedtak.flatMap { it.tilDto(kjøreliste) }
    }
}
