package no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext.kallKommerFra
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.mockBrukerContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles

class SikkerhetContextTest : IntegrationTest() {
    @AfterEach
    override fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `skal ikke godkjenne kall fra soknad-api for andre applikasjoner`() {
        mockBrukerContext("", azp_name = "prod-gcp:tilleggsstonader:tilleggsstonader-integrasjoner")
        assertThat(kallKommerFra(eksternApplikasjon.soknadApi)).isFalse
        clearBrukerContext()

        mockBrukerContext("", azp_name = "prod-gcp:teamfamilie:familie-ef-sak")
        assertThat(kallKommerFra(eksternApplikasjon.soknadApi)).isFalse
    }

    @Test
    internal fun `skal gjenkjenne kall fra soknad-api`() {
        mockBrukerContext("", azp_name = "prod-gcp:tilleggsstonader:tilleggsstonader-soknad-api")
        assertThat(kallKommerFra(eksternApplikasjon.soknadApi)).isTrue
        clearBrukerContext()
        mockBrukerContext("", azp_name = "dev-gcp:tilleggsstonader:tilleggsstonader-soknad-api")
        assertThat(kallKommerFra(eksternApplikasjon.soknadApi)).isTrue
        clearBrukerContext()
    }

    @Test
    internal fun `skal gjenkjenne kall fra en av appene som har tilgang`() {
        mockBrukerContext("", azp_name = "prod-gcp:tilleggsstonader:tilleggsstonader-soknad-api")
        assertThat(kallKommerFra(eksternApplikasjon.bidragGrunnlag, eksternApplikasjon.soknadApi)).isTrue
        assertThat(kallKommerFra(eksternApplikasjon.bidragGrunnlag)).isFalse()
    }
}

// Tester at eksternApplikasjon blir skrevet over av verdien fra application-local.yml
@ActiveProfiles("local")
class SikkerhetContextLocalTest : IntegrationTest() {
    @Test
    internal fun `skal gjenkjenne kall fra soknad-api-lokal`() {
        mockBrukerContext("", azp_name = "gcp:tilleggsstonader:tilleggsstonader-soknad-api-lokal")
        assertThat(kallKommerFra(eksternApplikasjon.soknadApi)).isTrue
    }
}
