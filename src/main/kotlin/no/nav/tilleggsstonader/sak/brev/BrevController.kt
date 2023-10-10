package no.nav.tilleggsstonader.sak.behandling

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.soknad.dokument.FamilieDokumentClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping(path = ["/api/brev/"])
@ProtectedWithClaims(issuer = "azuread")
class BrevController(private val familieDokumentClient: FamilieDokumentClient) {

    @PostMapping("lag-pdf")
    fun genererPdf(@RequestBody request: GenererPdfRequest): ByteArray {
        return familieDokumentClient.genererPdf(request.html)
    }

    data class GenererPdfRequest(val html: String)
}
