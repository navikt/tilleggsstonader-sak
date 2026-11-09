package no.nav.tilleggsstonader.sak.felles.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StønadstypeGjelderBarnKtTest {
    @Test
    fun `barnetilsyn har barn koblet til seg`() {
        assertThat(Stønadstype.BARNETILSYN.gjelderBarn()).isTrue
    }

    @Test
    fun `læremidler har ikke barn koblet til seg`() {
        assertThat(Stønadstype.LÆREMIDLER.gjelderBarn()).isFalse
    }

    @Test
    fun `skal vurdere adressebeskyttelse for barn for barnetilsyn og roah`() {
        val skalVurdereAdresseBeskyttelseStønadstyper =
            listOf(
                Stønadstype.BARNETILSYN,
                Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
                Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR,
            )

        skalVurdereAdresseBeskyttelseStønadstyper.forEach {
            assertThat(it.skalVurdereEvtBarnSinAdressebeskyttelse()).isTrue
        }

        Stønadstype.entries
            .filter { it !in skalVurdereAdresseBeskyttelseStønadstyper }
            .forEach { assertThat(it.skalVurdereEvtBarnSinAdressebeskyttelse()).isFalse }
    }
}
