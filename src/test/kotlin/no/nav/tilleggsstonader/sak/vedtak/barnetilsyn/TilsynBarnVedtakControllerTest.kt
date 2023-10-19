package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.util.ProblemDetailUtil.catchProblemDetailException
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.exchange
import java.util.UUID

class TilsynBarnVedtakControllerTest : IntegrationTest() {

    @Test
    fun `skal validere token`() {
        val exception = catchProblemDetailException {
            restTemplate.exchange<Map<String, Any>?>(
                localhost("api/vedtak/tilsyn-barn/${UUID.randomUUID()}"),
                HttpMethod.GET,
                HttpEntity(null, headers),
            )
        }

        assertThat(exception.httpStatus).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `skal returnere empty body når det ikke finnes noe lagret`() {
        headers.setBearerAuth(onBehalfOfToken())
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val response = restTemplate.exchange<Map<String, Any>?>(
            localhost("api/vedtak/tilsyn-barn/${behandling.id}"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNull()
    }
}
