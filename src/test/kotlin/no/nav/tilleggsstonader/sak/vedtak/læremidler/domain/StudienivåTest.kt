package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class StudienivåTest {

    @Nested
    inner class Prioritet {

        @Test
        fun `må ha unike prioriteter`() {
            val prioriteter = Studienivå.entries.map { it.prioritet }
            assertThat(prioriteter).hasSize(prioriteter.distinct().size)
        }

        @Test
        fun `høyere utdanning har høyere prioritet enn videregående`() {
            assertThat(Studienivå.entries.sortedBy { it.prioritet })
                .containsExactly(
                    Studienivå.HØYERE_UTDANNING,
                    Studienivå.VIDEREGÅENDE,
                )
        }
    }
}
