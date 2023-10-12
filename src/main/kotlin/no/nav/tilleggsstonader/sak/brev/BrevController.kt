package no.nav.tilleggsstonader.sak.brev

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Base64

@RestController
@RequestMapping("/api/brev")
@ProtectedWithClaims(issuer = "azuread")
class BrevController(private val familieDokumentClient: FamilieDokumentClient) {

    @PostMapping("lag-pdf")
    fun genererPdf(@RequestBody request: GenererPdfRequest): ByteArray {
        return Base64.getEncoder().encode(familieDokumentClient.genererPdf(request.html))
    }

    data class GenererPdfRequest(val html: String)
}
