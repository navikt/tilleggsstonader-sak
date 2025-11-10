package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

inline fun <reified T : Any> WebTestClient.ResponseSpec.expectOkWithBody(): T =
    expectStatus()
        .isOk
        .expectBody<T>()
        .returnResult()
        .responseBody!!

fun WebTestClient.ResponseSpec.expectOkEmpty() =
    expectStatus()
        .isOk
        .expectBody()
        .isEmpty

fun WebTestClient.ResponseSpec.expectBadRequestWithDetail(detail: String) =
    expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail")
        .isEqualTo(detail)
