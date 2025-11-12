package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.brev.GenererPdfRequest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient

class BrevKall(
    private val testklient: Testklient,
) {
    fun genererPdf(
        behandlingId: BehandlingId,
        dto: GenererPdfRequest,
    ) = apiRespons.genererPdf(behandlingId, dto).expectOkWithBody<ByteArray>()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = BrevApi()

    inner class BrevApi {
        fun genererPdf(
            behandlingId: BehandlingId,
            dto: GenererPdfRequest,
        ) = testklient.post("/api/brev/$behandlingId", dto)
    }
}
