package no.nav.tilleggsstonader.sak.integrasjonstest

import no.nav.tilleggsstonader.sak.IntegrationTest
import org.springframework.http.HttpMethod

class Testklient(
    val testkontekst: IntegrationTest,
) {
    fun get(uri: String) =
        with(testkontekst) {
            restTestClient
                .get()
                .uri(uri)
                .medOnBehalfOfToken()
                .exchange()
        }

    fun post(
        uri: String,
        body: Any? = null,
    ) = with(testkontekst) {
        restTestClient
            .post()
            .uri(uri)
            .let { if (body != null) it.body(body) else it }
            .medOnBehalfOfToken()
            .exchange()
    }

    fun put(
        uri: String,
        body: Any,
    ) = with(testkontekst) {
        restTestClient
            .put()
            .uri(uri)
            .body(body)
            .medOnBehalfOfToken()
            .exchange()
    }

    fun delete(
        uri: String,
        body: Any,
    ) = with(testkontekst) {
        restTestClient
            .method(HttpMethod.DELETE)
            .uri(uri)
            .body(body)
            .medOnBehalfOfToken()
            .exchange()
    }
}
