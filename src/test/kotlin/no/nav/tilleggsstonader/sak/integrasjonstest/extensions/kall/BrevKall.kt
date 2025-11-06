package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.brev.GenererPdfRequest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId

fun IntegrationTest.brevKall(
    behandlingId: BehandlingId,
    dto: GenererPdfRequest,
) {
    webTestClient
        .post()
        .uri("api/brev/$behandlingId")
        .bodyValue(dto)
        .medOnBehalfOfToken()
        .exchange()
        .expectStatus()
        .isOk
}
