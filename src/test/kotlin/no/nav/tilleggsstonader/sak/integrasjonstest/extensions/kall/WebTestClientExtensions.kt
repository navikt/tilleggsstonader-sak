package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import org.assertj.core.api.Assertions.assertThat
import org.springframework.http.HttpStatus
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

fun WebTestClient.ResponseSpec.expectProblemDetail(
    forventetStatus: HttpStatus,
    forventetDetail: String,
) = expectStatus()
    .isEqualTo(forventetStatus)
    .expectBody()
    .jsonPath("$.detail")
    .value<String> {
        assertThat(it).contains(forventetDetail)
    }
