package no.nav.tilleggsstonader.sak.infrastruktur

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import no.nav.tilleggsstonader.sak.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.exchange
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.collections.mapOf

class TestControllerTest : IntegrationTest() {
    val json = """{"tekst":"abc","dato":"2023-01-01","tidspunkt":"2023-01-01T12:00:03"}"""

    @Test
    fun `skal kunne hente json fra endepunkt`() {
        webTestClient
            .get()
            .uri("/api/test")
            .medOnBehalfOfToken()
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(json)
    }

    @Test
    fun `skal kunne sende inn object`() {
        val json =
            TestObject(
                tekst = "abc",
                dato = LocalDate.of(2023, 1, 1),
                tidspunkt = LocalDateTime.of(2023, 1, 1, 12, 0, 3),
            )

        webTestClient
            .post()
            .uri("/api/test")
            .bodyValue(json)
            .medOnBehalfOfToken()
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<TestObject>()
            .isEqualTo(json)
    }

    @Test
    fun `skal kunne sende inn object med json header`() {
        val json =
            TestObject(
                tekst = "abc",
                dato = LocalDate.of(2023, 1, 1),
                tidspunkt = LocalDateTime.of(2023, 1, 1, 12, 0, 3),
            )
        val jsonHeaders =
            HttpHeaders().apply {
                contentType = APPLICATION_JSON
                accept = listOf(APPLICATION_JSON)
            }

        webTestClient
            .post()
            .uri("/api/test")
            .headers { it.addAll(jsonHeaders) }
            .bodyValue(json)
            .medOnBehalfOfToken()
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<TestObject>()
            .isEqualTo(json)
    }

    @Test
    fun `skal håndtere ukjent feil`() {
        webTestClient
            .get()
            .uri("/api/test/error")
            .medOnBehalfOfToken()
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    @Test
    fun `endepunkt som ikke eksisterer skal kaste 404`() {
        webTestClient
            .get()
            .uri("/api/eksistererIkke")
            .medOnBehalfOfToken()
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody()
            .json(
                """
                {"type":"about:blank","title":"Not Found","status":404,"detail":"No static resource api/eksistererIkke.","instance":"/api/eksistererIkke"}
                """.trimIndent(),
            )
    }

    @Nested
    inner class PrimtiveFelter {
        val headers =
            HttpHeaders().apply {
                contentType = APPLICATION_JSON
                accept = listOf(APPLICATION_JSON)
            }

        @Test
        fun `skal feile hvis påkrevd booleanfelt er null`() {
            webTestClient
                .post()
                .uri("/api/test/boolean")
                .bodyValue(mapOf<Any, Any>())
                .medOnBehalfOfToken()
                .exchange()
                .expectStatus()
                .is5xxServerError
                .expectBody()
                .jsonPath("$.detail")
                .value<String> {
                    assertThat(it).contains("Missing required creator property 'verdi'")
                }
        }

        @Test
        fun `skal feile hvis påkrevd booleanfelt sendes inn som null`() {
            webTestClient
                .post()
                .uri("/api/test/boolean")
                .bodyValue(mapOf("verdi" to null))
                .medOnBehalfOfToken()
                .exchange()
                .expectStatus()
                .is5xxServerError
                .expectBody()
                .jsonPath("$.detail")
                .value<String> {
                    assertThat(it).startsWith("JSON parse error: Cannot map `null` into type `boolean`")
                }
        }

        @Test
        fun `skal ikke feile hvis påkrevd booleanfelt med default-verdi er null `() {
            webTestClient
                .post()
                .uri("/api/test/boolean-default")
                .bodyValue(mapOf<Any, Any>())
                .medOnBehalfOfToken()
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<TestObjectBooleanDefault>()
                .isEqualTo(TestObjectBooleanDefault(true))
        }

        @Test
        fun `skal ikke feile hvis valgfritt booleanfelt er null `() {
            webTestClient
                .post()
                .uri("/api/test/boolean-optional")
                .bodyValue(mapOf<Any, Any>())
                .medOnBehalfOfToken()
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<TestObjectBooleanOptional>()
                .isEqualTo(TestObjectBooleanOptional(null))
        }
    }

    @Nested
    inner class EndepunktMedTokenValidering {
        @Test
        fun `autorisert token, men mangler saksbehandler-relatert rolle`() {
            listOf(rolleConfig.kode6, rolleConfig.kode7, rolleConfig.egenAnsatt).forEach {
                medBrukercontext(rolle = it) {
                    webTestClient
                        .get()
                        .uri("/api/test/azuread")
                        .medOnBehalfOfToken()
                        .exchange()
                        .expectStatus()
                        .isForbidden
                }
            }
        }

        @Test
        fun `autorisert token med saksbehandler-relatert rolle kan gjøre kall`() {
            listOf(rolleConfig.saksbehandlerRolle, rolleConfig.beslutterRolle, rolleConfig.veilederRolle).forEach {
                medBrukercontext(rolle = it) {
                    webTestClient
                        .get()
                        .uri("/api/test/azuread")
                        .medOnBehalfOfToken()
                        .exchange()
                        .expectStatus()
                        .isOk
                        .expectBody<String>()
                }
            }
        }
    }
}

@RestController
@RequestMapping("/api/test")
@Unprotected
class TestController {
    @GetMapping
    fun get(): TestObject =
        TestObject(
            tekst = "abc",
            dato = LocalDate.of(2023, 1, 1),
            tidspunkt = LocalDateTime.of(2023, 1, 1, 12, 0, 3),
        )

    @PostMapping
    fun post(
        @RequestBody testObject: TestObject,
    ): TestObject = testObject

    @GetMapping("error")
    fun error() {
        error("error")
    }

    @GetMapping("azuread")
    @ProtectedWithClaims(issuer = "azuread")
    fun getMedAzureAd(): TestObject = get()
}

@RestController
@RequestMapping("/api/test")
@Unprotected
class TestBooleanController {
    @ExceptionHandler(HttpMessageConversionException::class)
    fun handleThrowable(throwable: HttpMessageConversionException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, throwable.message)

    @PostMapping("/boolean")
    fun boolean(
        @RequestBody testObject: TestObjectBoolean,
    ): TestObjectBoolean = testObject

    @PostMapping("/boolean-default")
    fun booleanDefault(
        @RequestBody testObject: TestObjectBooleanDefault,
    ): TestObjectBooleanDefault = testObject

    @PostMapping("/boolean-optional")
    fun booleanOptional(
        @RequestBody testObject: TestObjectBooleanOptional,
    ): TestObjectBooleanOptional = testObject
}

data class TestObject(
    val tekst: String,
    val dato: LocalDate,
    val tidspunkt: LocalDateTime,
)

data class TestObjectBoolean(
    val verdi: Boolean,
)

data class TestObjectBooleanDefault(
    val verdi: Boolean = true,
)

data class TestObjectBooleanOptional(
    val verdi: Boolean?,
)
