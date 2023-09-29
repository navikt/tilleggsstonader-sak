package no.nav.tilleggsstonader.sak.opplysninger.pdl

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.util.FileUtil.readFile
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
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

        val response = pdlClient.hentPersonForelderBarnRelasjon(listOf("11111122222"))

        assertThat(response["11111122222"]?.fødsel?.get(0)?.fødselsdato).isEqualTo(LocalDate.of(1966, 11, 18))
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
        val response = pdlClient.hentAktørIder("12345")
        assertThat(response.identer.first().ident).isEqualTo("12345678901")
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
        assertThat(Assertions.catchThrowable { pdlClient.hentPersonForelderBarnRelasjon(listOf("")) })
            .hasMessageStartingWith("Feil ved henting av")
            .isInstanceOf(PdlRequestException::class.java)
    }

    @Test
    fun `pdlClient håndterer response for bolk-query mot pdl-tjenesten der data er null og har errors`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                .willReturn(okJson(readFile("pdl/pdlBolkErrorResponse_nullData.json"))),
        )
        assertThat(Assertions.catchThrowable { pdlClient.hentPersonForelderBarnRelasjon(listOf("")) })
            .hasMessageStartingWith("Data er null fra PDL")
            .isInstanceOf(PdlRequestException::class.java)
    }

    @Nested
    inner class HentIdenterBolk {

        @Test
        fun `håndterer response for uthenting av identer i bolk`() {
            wiremockServerItem.stubFor(
                post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                    .willReturn(okJson(readFile("pdl/hent_identer_bolk.json"))),
            )
            val response = pdlClient.hentIdenterBolk(listOf("12345"))
            assertThat(response["12345678910"]?.ident).isEqualTo("11223344556677")
            assertThat(response["12345678911"]?.ident).isEqualTo("12345678911")
            assertThat(response["test"]?.ident).isEqualTo("test")
        }

        @Test
        fun `feiler hvis antall identer overstiger MAKS_ANTALL_IDENTER`() {
            assertThrows<Feil> { pdlClient.hentIdenterBolk((1..PdlClient.MAKS_ANTALL_IDENTER + 1).map { "$it" }) }
        }

        @Test
        fun `kjører feilfritt hvis antall identer er lik MAKS_ANTALL_IDENTER`() {
            wiremockServerItem.stubFor(
                post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                    .willReturn(okJson(readFile("pdl/hent_identer_bolk.json"))),
            )
            assertDoesNotThrow { pdlClient.hentIdenterBolk((1..PdlClient.MAKS_ANTALL_IDENTER).map { "$it" }) }
        }

        @Test
        fun `skal håndtere hentIdenter hvor det ikke finnes en person`() {
            wiremockServerItem.stubFor(
                post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                    .willReturn(okJson(readFile("pdl/hent_identer_finnes_ikke.json"))),
            )

            assertThrows<PdlNotFoundException> { pdlClient.hentPersonidenter("12345678901") }
        }
    }
}
