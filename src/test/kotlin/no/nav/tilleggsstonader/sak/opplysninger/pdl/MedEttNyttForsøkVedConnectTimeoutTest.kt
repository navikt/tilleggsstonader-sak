package no.nav.tilleggsstonader.sak.opplysninger.pdl

import org.apache.hc.client5.http.ConnectTimeoutException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.web.client.ResourceAccessException
import java.net.SocketTimeoutException

class MedEttNyttForsøkVedConnectTimeoutTest {
    private val connectTimeout = ResourceAccessException("connect", ConnectTimeoutException("timeout"))
    private val readTimeout = ResourceAccessException("read", SocketTimeoutException("timeout"))

    @Test
    fun `prøver på nytt én gang ved connect-timeout`() {
        var antallKall = 0

        val resultat =
            medEttNyttForsøkVedConnectTimeout {
                antallKall++
                if (antallKall == 1) throw connectTimeout
                "ok"
            }

        assertThat(resultat).isEqualTo("ok")
        assertThat(antallKall).isEqualTo(2)
    }

    @Test
    fun `kaster videre hvis andre forsøk også feiler`() {
        var antallKall = 0

        assertThatThrownBy {
            medEttNyttForsøkVedConnectTimeout {
                antallKall++
                throw connectTimeout
            }
        }.isSameAs(connectTimeout)
        assertThat(antallKall).isEqualTo(2)
    }

    @Test
    fun `prøver ikke på nytt ved read-timeout`() {
        var antallKall = 0

        assertThatThrownBy {
            medEttNyttForsøkVedConnectTimeout {
                antallKall++
                throw readTimeout
            }
        }.isSameAs(readTimeout)
        assertThat(antallKall).isEqualTo(1)
    }
}
