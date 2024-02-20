package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.security.mock.oauth2.http.objectMapper
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettDtoUtil.iverksettDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.util.UUID

class IverksettClientTest {

    @Test
    fun `skal ignorere CONFLICT`() {
        wiremockServerItem.stubFor(
            post(anyUrl()).willReturn(aResponse().withStatus(HttpStatus.CONFLICT.value())),
        )
        assertThatCode {
            client.iverksett(iverksettDto())
        }.doesNotThrowAnyException()
    }

    @Test
    fun `skal kaste feil ved BAD_REQUEST`() {
        wiremockServerItem.stubFor(
            post(anyUrl()).willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())),
        )
        val exception = catchThrowableOfType<HttpClientErrorException.BadRequest> {
            client.iverksett(iverksettDto())
        }
        assertThat(exception).isNotNull()
    }

    @Test
    fun `skal hente status for gitt behandlingId og iverksettingId`() {
        val eksternFagsakId = 10L
        val behandlingId = UUID.randomUUID()
        val iverksettingId = UUID.randomUUID()
        val json = objectMapper.writeValueAsString("OK")
        wiremockServerItem.stubFor(
            get(WireMock.urlEqualTo("/api/iverksetting/$eksternFagsakId/$behandlingId/$iverksettingId/status"))
                .willReturn(okJson(json)),
        )

        assertThat(client.hentStatus(eksternFagsakId, behandlingId, iverksettingId)).isEqualTo(IverksettStatus.OK)
    }

    companion object {

        private val restTemplate: RestTemplate = RestTemplateBuilder().build()
        lateinit var client: IverksettClient
        lateinit var wiremockServerItem: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServerItem.start()
            client = IverksettClient(URI.create(wiremockServerItem.baseUrl()), restTemplate)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wiremockServerItem.stop()
        }
    }
}
