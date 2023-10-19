package no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet

import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.mockBrukerContext
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class SikkerhetContextTest {

    @Test
    internal fun `skal ikke godkjenne kall fra familie-ef-mottak for andre applikasjoner`() {
        mockBrukerContext("", azp_name = "prod-gcp:tilleggsstonader:tilleggsstonader-integrasjoner")
        Assertions.assertThat(SikkerhetContext.kallKommerFraSoknadApi()).isFalse
        clearBrukerContext()

        mockBrukerContext("", azp_name = "prod-gcp:teamfamilie:familie-ef-sak")
        Assertions.assertThat(SikkerhetContext.kallKommerFraSoknadApi()).isFalse
        clearBrukerContext()
    }

    @Test
    internal fun `skal gjenkjenne kall fra familie-ef-mottak`() {
        mockBrukerContext("", azp_name = "prod-gcp:tilleggsstonader:tilleggsstonader-soknad-api")
        Assertions.assertThat(SikkerhetContext.kallKommerFraSoknadApi()).isTrue
        clearBrukerContext()
        mockBrukerContext("", azp_name = "dev-gcp:tilleggsstonader:tilleggsstonader-soknad-api")
        Assertions.assertThat(SikkerhetContext.kallKommerFraSoknadApi()).isTrue
        clearBrukerContext()
    }
}
