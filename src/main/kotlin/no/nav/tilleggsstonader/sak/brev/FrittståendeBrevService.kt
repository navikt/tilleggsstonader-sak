package no.nav.tilleggsstonader.sak.brev

import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service

@Service
class FrittståendeBrevService(
    private val familieDokumentClient: FamilieDokumentClient,
) {

    fun lagFrittståendeSanitybrev(
        request: GenererPdfRequest,
    ): ByteArray {
        val signatur = SikkerhetContext.hentSaksbehandlerNavn(strict = true)

        val htmlMedSignatur = BrevUtil.settInnSaksbehandlerSignatur(request.html, signatur)

        return familieDokumentClient.genererPdf(htmlMedSignatur)
    }
}
