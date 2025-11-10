package no.nav.tilleggsstonader.sak.migrering.arena

import no.nav.tilleggsstonader.kontrakter.arena.vedtak.Rettighet
import no.nav.tilleggsstonader.sak.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EksternArenaControllerTest : IntegrationTest() {
    @Test
    fun `skal kunne sende inn rettighetstype som er mappet`() {
        val response = kall.arena.status(ArenaFinnesPersonRequest("ident", Rettighet.TILSYN_BARN.kodeArena))
        assertThat(response.finnes).isTrue()
    }

    @Test
    fun `skal kaste feil hvis man sender inn rettighet som ikke er mappet`() {
        kall.arena
            .statusResponse(ArenaFinnesPersonRequest("ident", Rettighet.REISE.kodeArena))
            .expectStatus()
            .is5xxServerError
            .expectBody()
            .jsonPath("$.detail")
            .isEqualTo("Har ikke lagt inn mapping av stønadstype for REISE")
    }
}
