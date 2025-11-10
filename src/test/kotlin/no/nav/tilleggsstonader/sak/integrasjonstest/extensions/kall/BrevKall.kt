package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.brev.GenererPdfRequest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId

class BrevKall(
    private val test: IntegrationTest,
) {
    fun brev(
        behandlingId: BehandlingId,
        dto: GenererPdfRequest,
    ) = brevResponse(behandlingId, dto).expectOkWithBody<ByteArray>()

    fun brevResponse(
        behandlingId: BehandlingId,
        dto: GenererPdfRequest,
    ) = with(test) {
        webTestClient
            .post()
            .uri("api/brev/$behandlingId")
            .bodyValue(dto)
            .medOnBehalfOfToken()
            .exchange()
    }
}
