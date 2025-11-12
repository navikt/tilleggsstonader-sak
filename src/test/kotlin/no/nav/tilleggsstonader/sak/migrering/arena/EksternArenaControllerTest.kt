package no.nav.tilleggsstonader.sak.migrering.arena

import no.nav.tilleggsstonader.kontrakter.arena.vedtak.Rettighet
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectProblemDetail
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class EksternArenaControllerTest : IntegrationTest() {
    @Test
    fun `skal kunne sende inn rettighetstype som er mappet`() {
        val response = kall.arena.status(ArenaFinnesPersonRequest("ident", Rettighet.TILSYN_BARN.kodeArena))
        assertThat(response.finnes).isTrue()
    }

    @Test
    fun `skal kaste feil hvis man sender inn rettighet som ikke er mappet`() {
        kall.arena.apiRespons
            .status(ArenaFinnesPersonRequest("ident", Rettighet.REISE.kodeArena))
            .expectProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Har ikke lagt inn mapping av stønadstype for REISE")
    }
}
