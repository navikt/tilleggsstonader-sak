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
}
