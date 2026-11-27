package no.nav.tilleggsstonader.sak.infrastruktur.exception

import org.apache.hc.client5.http.ConnectTimeoutException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.client.ResourceAccessException
import java.net.SocketTimeoutException

class ApiExceptionHandlerTest {
    private val handler = ApiExceptionHandler()

    @Test
    fun `timeout pakket inn i IntegrasjonException gir 503 med melding om hendelsen`() {
        val timeout = ResourceAccessException("timeout", SocketTimeoutException("Read timed out"))
        val feil = integrasjonException(RuntimeException("Feil ved kall", timeout))

        val problemDetail = handler.handleThrowable(feil)

        assertThat(problemDetail.status).isEqualTo(503)
        assertThat(problemDetail.detail).startsWith("Feilet henting av aktiviteter.")
    }

    @Test
    fun `connect-timeout pakket inn i IntegrasjonException gir 503`() {
        val timeout = ResourceAccessException("timeout", ConnectTimeoutException("Connect timed out"))
        val feil = integrasjonException(RuntimeException("Feil ved kall", timeout))

        assertThat(handler.handleThrowable(feil).status).isEqualTo(503)
    }

    @Test
    fun `annen feil i IntegrasjonException gir 500`() {
        val feil = integrasjonException(IllegalStateException("Mangler body"))

        val problemDetail = handler.handleThrowable(feil)

        assertThat(problemDetail.status).isEqualTo(500)
        assertThat(problemDetail.detail).startsWith("Feilet henting av aktiviteter.")
    }

    @Test
    fun `timeout som ikke er pakket inn gir 503`() {
        val feil = RuntimeException("Feil ved kall", ResourceAccessException("timeout", SocketTimeoutException()))

        assertThat(handler.handleThrowable(feil).status).isEqualTo(503)
    }

    private fun integrasjonException(årsak: Throwable) =
        IntegrasjonException(
            hendelse = "henting av aktiviteter",
            tjeneste = IntegrasjonsTjeneste.AKTIVITETER,
            throwable = årsak,
        )
}
