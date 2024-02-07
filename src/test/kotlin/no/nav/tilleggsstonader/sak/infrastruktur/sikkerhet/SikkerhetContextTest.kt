package no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet

import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.mockBrukerContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SikkerhetContextTest {

    @Test
    internal fun `skal ikke godkjenne kall fra soknad-api for andre applikasjoner`() {
        mockBrukerContext("", azp_name = "prod-gcp:tilleggsstonader:tilleggsstonader-integrasjoner")
        assertThat(SikkerhetContext.kallKommerFra(EksternApplikasjon.SOKNAD_API)).isFalse
        clearBrukerContext()

        mockBrukerContext("", azp_name = "prod-gcp:teamfamilie:familie-ef-sak")
        assertThat(SikkerhetContext.kallKommerFra(EksternApplikasjon.SOKNAD_API)).isFalse
        clearBrukerContext()
    }

    @Test
    internal fun `skal gjenkjenne kall fra soknad-api`() {
        mockBrukerContext("", azp_name = "prod-gcp:tilleggsstonader:tilleggsstonader-soknad-api")
        assertThat(SikkerhetContext.kallKommerFra(EksternApplikasjon.SOKNAD_API)).isTrue
        clearBrukerContext()
        mockBrukerContext("", azp_name = "dev-gcp:tilleggsstonader:tilleggsstonader-soknad-api")
        assertThat(SikkerhetContext.kallKommerFra(EksternApplikasjon.SOKNAD_API)).isTrue
        clearBrukerContext()
    }
}
