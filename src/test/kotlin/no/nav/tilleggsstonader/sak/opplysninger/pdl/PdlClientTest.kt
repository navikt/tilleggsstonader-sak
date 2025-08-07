package no.nav.tilleggsstonader.sak.opplysninger.pdl

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.util.FileUtil.readFile
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.LocalDate

class PdlClientTest {
    companion object {
        private val restTemplate: RestTemplate = RestTemplateBuilder().build()
        lateinit var pdlClient: PdlClient
        lateinit var wiremockServerItem: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(wireMockConfig().dynamicPort())
            wiremockServerItem.start()
            pdlClient = PdlClient(URI.create(wiremockServerItem.baseUrl()), restTemplate)
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
    fun `pdlClient håndterer response for søker-query mot pdl-tjenesten riktig`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                .willReturn(okJson(readFile("pdl/søker.json"))),
        )

        val response = pdlClient.hentSøker("")

        assertThat(response.bostedsadresse[0].vegadresse?.adressenavn).isEqualTo("INNGJERDSVEGEN")
    }

    @Test
    fun `pdlClient håndterer response for andreForeldre-query mot pdl-tjenesten riktig`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                .willReturn(okJson(readFile("pdl/andreForeldre.json"))),
        )

        val response = pdlClient.hentAndreForeldre(listOf("11111122222"))

        assertThat(response["11111122222"]?.bostedsadresse?.get(0)?.gyldigFraOgMed).isEqualTo(
            LocalDate.of(1966, 11, 18),
        )
    }

    @Test
    fun `pdlClient håndterer response for barn-query mot pdl-tjenesten riktig`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                .willReturn(okJson(readFile("pdl/barn.json"))),
        )

        val response = pdlClient.hentBarn(listOf("11111122222"))

        assertThat(response["11111122222"]?.fødselsdato?.get(0)?.fødselsdato).isEqualTo(LocalDate.of(1966, 11, 18))
    }

    @Test
    fun `pdlClient håndterer response for personKortBolk-query mot pdl-tjenesten riktig`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                .willReturn(okJson(readFile("pdl/person_kort_bolk.json"))),
        )

        val response = pdlClient.hentPersonKortBolk(listOf("11111122222"))

        assertThat(response["11111122222"]?.navn?.get(0)?.fornavn).isEqualTo("BRÅKETE")
    }

    @Test
    fun `pdlClient håndterer response for uthenting av identer`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                .willReturn(okJson(readFile("pdl/hent_identer.json"))),
        )
        val response = pdlClient.hentPersonidenter("12345")
        assertThat(response.identer).containsExactlyInAnyOrder(
            PdlIdent("10987654321", false, "FOLKEREGISTERIDENT"),
            PdlIdent("12345678901", false, "AKTORID"),
        )
    }

    @Test
    fun `pdlClient håndterer response for søker-query mot pdl-tjenesten der person i data er null`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                .willReturn(okJson("{\"data\": {}}")),
        )
        assertThat(Assertions.catchThrowable { pdlClient.hentSøker("") })
            .hasMessageStartingWith("Manglende ")
            .isInstanceOf(PdlRequestException::class.java)
    }

    @Test
    fun `pdlClient håndterer response for søker-query mot pdl-tjenesten der data er null og har errors`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                .willReturn(okJson(readFile("pdl/pdlErrorResponse.json"))),
        )
        assertThat(Assertions.catchThrowable { pdlClient.hentSøker("") })
            .hasMessageStartingWith("Feil ved henting av")
            .isInstanceOf(PdlRequestException::class.java)
    }

    @Test
    fun `pdlClient håndterer response for bolk-query mot pdl-tjenesten der person er null og har errors`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                .willReturn(okJson(readFile("pdl/pdlBolkErrorResponse.json"))),
        )
        assertThat(Assertions.catchThrowable { pdlClient.hentBarn(listOf("")) })
            .hasMessageStartingWith("Feil ved henting av")
            .isInstanceOf(PdlRequestException::class.java)
    }

    @Test
    fun `pdlClient håndterer response for bolk-query mot pdl-tjenesten der data er null og har errors`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                .willReturn(okJson(readFile("pdl/pdlBolkErrorResponse_nullData.json"))),
        )
        assertThat(Assertions.catchThrowable { pdlClient.hentBarn(listOf("")) })
            .hasMessageStartingWith("Data er null fra PDL")
            .isInstanceOf(PdlRequestException::class.java)
    }
}
