package no.nav.tilleggsstonader.sak.infrastruktur.config

import no.nav.security.token.support.core.api.Unprotected
import no.nav.tilleggsstonader.sak.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

class AddSecurityHeadersFiltersTest : IntegrationTest() {
    @Test
    internal fun `verifiser ping svarer med pong, mimetype sniffing deaktivert og cache-control`() {
        webTestClient
            .get()
            .uri("/api/ping")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<String>()
            .consumeWith {
                assertThat(it.responseBody).isEqualTo("pong")
                assertThat(it.responseHeaders["X-Content-Type-Options"]).contains("nosniff")
                assertThat(it.responseHeaders["Cache-Control"]).contains("private, max-age=0, no-cache, no-store")
            }
    }
}

@RestController
@RequestMapping("/api/ping")
@Unprotected
class PingController {
    @GetMapping
    fun get() = "pong"
}
