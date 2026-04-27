package no.nav.tilleggsstonader.sak.arbeidsfordeling

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import no.nav.tilleggsstonader.sak.util.FileUtil.readFile
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient

class OppfolgingsenhetClientTest {
    companion object {
        private val restClient: RestClient = RestClient.builder().build()

        lateinit var client: OppfolgingsenhetClient
        lateinit var wiremockServerItem: WireMockServer

        val forventetUrl = "/veilarboppfolging/api/graphql"

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(wireMockConfig().dynamicPort())
            wiremockServerItem.start()
            client = OppfolgingsenhetClient(wiremockServerItem.baseUrl(), "tilleggsstonader-sak", restClient)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wiremockServerItem.stop()
        }
    }

    @AfterEach
    fun tearDownEachTest() {
        wiremockServerItem.resetAll()
    }

    @Test
    fun `henter nav-kontor fra oppfolgingsenhet`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo(forventetUrl))
                .withHeader("Nav-Consumer-Id", equalTo("tilleggsstonader-sak"))
                .willReturn(okJson(readFile("arbeidsfordeling/oppfolgingsenhet_ok.json"))),
        )

        val navKontor = client.hentOppfølgingsenhet("12345678901")

        assertThat(navKontor).isEqualTo("1234")
    }

    @Test
    fun `returnerer null nar enhet er null`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo(forventetUrl))
                .willReturn(okJson(readFile("arbeidsfordeling/oppfolgingsenhet_null_enhet.json"))),
        )

        val navKontor = client.hentOppfølgingsenhet("12345678901")

        assertThat(navKontor).isNull()
    }

    @Test
    fun `kaster feil ved errors i respons`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo(forventetUrl))
                .willReturn(okJson(readFile("arbeidsfordeling/oppfolgingsenhet_errors.json"))),
        )

        assertThat(Assertions.catchThrowable { client.hentOppfølgingsenhet("12345678901") })
            .hasMessageStartingWith("Feil ved henting av oppfolgingsenhet")
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `kaster feil nar data er null`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo(forventetUrl))
                .willReturn(okJson(readFile("arbeidsfordeling/oppfolgingsenhet_null_data.json"))),
        )

        assertThat(Assertions.catchThrowable { client.hentOppfølgingsenhet("12345678901") })
            .hasMessageStartingWith("Data er null fra oppfolgingsenhet")
            .isInstanceOf(IllegalStateException::class.java)
    }
}
