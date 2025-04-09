package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class OppgaveUtilTest {
    @Nested
    inner class LagFristForOppgave {
        private fun LocalDateTime.kveld(): LocalDateTime = this.withHour(20)

        private fun LocalDateTime.morgen(): LocalDateTime = this.withHour(8)

        private val torsdag = LocalDateTime.of(2021, 4, 1, 12, 0)
        private val fredag = LocalDateTime.of(2021, 4, 2, 12, 0)
        private val lørdag = LocalDateTime.of(2021, 4, 3, 12, 0)
        private val søndag = LocalDateTime.of(2021, 4, 4, 12, 0)
        private val mandag = LocalDateTime.of(2021, 4, 5, 12, 0)

        private val fredagFrist = LocalDate.of(2021, 4, 2)
        private val mandagFrist = LocalDate.of(2021, 4, 5)
        private val tirsdagFrist = LocalDate.of(2021, 4, 6)
        private val onsdagFrist = LocalDate.of(2021, 4, 7)

        @Test
        fun `Skal sette frist for oppgave`() {
            val frister =
                listOf<Pair<LocalDateTime, LocalDate>>(
                    Pair(torsdag.morgen(), fredagFrist),
                    Pair(torsdag.kveld(), mandagFrist),
                    Pair(fredag.morgen(), mandagFrist),
                    Pair(fredag.kveld(), tirsdagFrist),
                    Pair(lørdag.morgen(), tirsdagFrist),
                    Pair(lørdag.kveld(), tirsdagFrist),
                    Pair(søndag.morgen(), tirsdagFrist),
                    Pair(søndag.kveld(), tirsdagFrist),
                    Pair(mandag.morgen(), tirsdagFrist),
                    Pair(mandag.kveld(), onsdagFrist),
                )

            frister.forEach {
                assertThat(OppgaveUtil.lagFristForOppgave(it.first)).isEqualTo(it.second)
            }
        }
    }
}
