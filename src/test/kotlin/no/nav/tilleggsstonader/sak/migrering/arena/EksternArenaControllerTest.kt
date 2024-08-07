package no.nav.tilleggsstonader.sak.migrering.arena

import no.nav.tilleggsstonader.kontrakter.arena.vedtak.Rettighet
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import no.nav.tilleggsstonader.sak.util.ProblemDetailUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.exchange

class EksternArenaControllerTest : IntegrationTest() {

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(clientCredential(EksternApplikasjon.ARENA.namespaceAppNavn, true))
    }

    @Test
    fun `skal kunne sende inn rettighetstype som er mappet`() {
        val response = hentStatus("ident", Rettighet.TILSYN_BARN)
        assertThat(response.finnes).isTrue()
    }

    @Test
    fun `skal kaste feil hvis man sender inn rettighet som ikke er mappet`() {
        val exception = ProblemDetailUtil.catchProblemDetailException {
            hentStatus("ident", Rettighet.REISE)
        }

        assertThat(exception.detail.detail)
            .isEqualTo("Har ikke lagt inn mapping av stønadstype for REISE")
    }

    private fun hentStatus(ident: String, rettighet: Rettighet): ArenaFinnesPersonResponse {
        val request = mapOf(
            "ident" to ident,
            "rettighet" to rettighet.kodeArena,
        )
        return restTemplate.exchange<ArenaFinnesPersonResponse>(
            localhost("api/ekstern/arena/status"),
            HttpMethod.POST,
            HttpEntity(request, headers),
        ).body!!
    }
}
