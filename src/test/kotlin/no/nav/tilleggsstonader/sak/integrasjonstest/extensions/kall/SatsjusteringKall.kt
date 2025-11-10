package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId

// Må kjøres med utvikler-rolle
class SatsjusteringKall(
    private val test: IntegrationTest,
) {
    fun satsjustering(stønadstype: Stønadstype): List<BehandlingId> = satsjusteringResponse(stønadstype).expectOkWithBody()

    fun satsjusteringResponse(stønadstype: Stønadstype) =
        with(test) {
            webTestClient
                .post()
                .uri("/api/forvaltning/satsjustering/$stønadstype")
                .medOnBehalfOfToken()
                .exchange()
        }
}
