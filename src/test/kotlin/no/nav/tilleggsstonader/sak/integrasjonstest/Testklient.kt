package no.nav.tilleggsstonader.sak.integrasjonstest

import no.nav.tilleggsstonader.sak.IntegrationTest
import org.springframework.http.HttpMethod

class Testklient(
    val testkontekst: IntegrationTest,
) {
    fun get(uri: String) =
        with(testkontekst) {
            webTestClient
                .get()
                .uri(uri)
                .medOnBehalfOfToken()
                .exchange()
        }

    fun post(
        uri: String,
        body: Any? = null,
    ) = with(testkontekst) {
        webTestClient
            .post()
            .uri(uri)
            .let { if (body != null) it.bodyValue(body) else it }
            .medOnBehalfOfToken()
            .exchange()
    }

    fun put(
        uri: String,
        body: Any,
    ) = with(testkontekst) {
        webTestClient
            .put()
            .uri(uri)
            .bodyValue(body)
            .medOnBehalfOfToken()
            .exchange()
    }

    fun delete(
        uri: String,
        body: Any,
    ) = with(testkontekst) {
        webTestClient
            .method(HttpMethod.DELETE)
            .uri(uri)
            .bodyValue(body)
            .medOnBehalfOfToken()
            .exchange()
    }
}
