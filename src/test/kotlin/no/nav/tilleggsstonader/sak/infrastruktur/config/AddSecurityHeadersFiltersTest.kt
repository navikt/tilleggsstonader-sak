package no.nav.tilleggsstonader.sak.infrastruktur.config

import no.nav.security.token.support.core.api.Unprotected
import no.nav.tilleggsstonader.sak.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.getForEntity

class AddSecurityHeadersFiltersTest : IntegrationTest() {

    @Test
    internal fun `ping svarer med pong`() {
        val response = restTemplate.getForEntity<String>(localhost("api/ping"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!).isEqualTo("pong")
    }

    @Test
    fun `mime-type sniffing er deaktivert i header`() {
        val response = restTemplate.getForEntity<String>(localhost("api/ping"))

        assertThat(response.headers["X-Content-Type-Options"]).contains("nosniff")
    }

    @Test
    fun `headers har riktig cache-control`() {
        val response = restTemplate.getForEntity<String>(localhost("api/ping"))

        assertThat(response.headers["Cache-Control"]).contains("private, max-age=0, no-cache, no-store")
    }
}

@RestController
@RequestMapping("/api/ping")
@Unprotected
class PingController {

    @GetMapping
    fun get() = "pong"
}
