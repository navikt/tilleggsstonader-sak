package no.nav.tilleggsstonader.sak.migrering.arena

import no.nav.tilleggsstonader.kontrakter.arena.vedtak.Rettighet
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.kall.hentStatusFraArena
import no.nav.tilleggsstonader.sak.kall.hentStatusFraArenaKall
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EksternArenaControllerTest : IntegrationTest() {
    @Test
    fun `skal kunne sende inn rettighetstype som er mappet`() {
        val response = hentStatusFraArena(ArenaFinnesPersonRequest("ident", Rettighet.TILSYN_BARN.kodeArena))
        assertThat(response.finnes).isTrue()
    }

    @Test
    fun `skal kaste feil hvis man sender inn rettighet som ikke er mappet`() {
        hentStatusFraArenaKall(ArenaFinnesPersonRequest("ident", Rettighet.REISE.kodeArena))
            .expectStatus()
            .is5xxServerError
            .expectBody()
            .jsonPath("$.detail")
            .isEqualTo("Har ikke lagt inn mapping av stønadstype for REISE")
    }
}
