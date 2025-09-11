package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import org.springframework.test.web.reactive.server.expectBody

// Må kjøres med utvikler-rolle
fun IntegrationTest.kjørSatsjusteringForStønadstypeKall(stønadstype: Stønadstype) =
    webTestClient
        .post()
        .uri("/api/forvaltning/satsjustering/$stønadstype")
        .medOnBehalfOfToken()
        .exchange()

fun IntegrationTest.kjørSatsjusteringForStønadstype(stønadstype: Stønadstype) =
    kjørSatsjusteringForStønadstypeKall(stønadstype)
        .expectBody<List<BehandlingId>>()
        .returnResult()
        .responseBody!!
