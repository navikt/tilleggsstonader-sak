package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import org.assertj.core.api.Assertions.assertThat
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.expectBody

inline fun <reified T : Any> RestTestClient.ResponseSpec.expectOkWithBody(): T =
    expectStatus()
        .isOk
        .expectBody<T>()
        .returnResult()
        .responseBody!!

fun RestTestClient.ResponseSpec.expectOkEmpty() =
    expectStatus()
        .isOk
        .expectBody()
        .isEmpty

fun RestTestClient.ResponseSpec.expectProblemDetail(
    forventetStatus: HttpStatus,
    forventetDetail: String,
) = expectStatus()
    .isEqualTo(forventetStatus)
    .expectBody()
    .jsonPath("$.detail")
    .value<String> {
        assertThat(it).contains(forventetDetail)
    }
